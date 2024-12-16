package me.ztiany.rtmp.audio

import android.annotation.SuppressLint
import android.media.*
import me.ztiany.lib.avbase.utils.Directory
import me.ztiany.lib.avbase.utils.closeSafely
import me.ztiany.rtmp.common.Packet
import me.ztiany.rtmp.common.PacketDataCallback
import me.ztiany.rtmp.common.TYPE_AUDIO_DATA
import me.ztiany.rtmp.common.TYPE_AUDIO_INFO
import timber.log.Timber
import java.io.FileWriter
import kotlin.concurrent.thread

class AACAudioSource {

    var packetDataCallback: PacketDataCallback? = null

    @Volatile private var isStopped = true

    private var mediaCodec: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var minBufferSize = 0

    @Volatile private var inCanRelease = false
    @Volatile private var outCanRelease = false

    private var printed = false

    private var startTime = 0L

    @SuppressLint("MissingPermission")
    fun start() {
        isStopped = false
        printed = false

        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            44100,
            2/*双声道*/
        )
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)//AAC 的质量
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)//一秒的码率 aac，128kbps 为 一般质量音频

        try {
            minBufferSize = AudioRecord.getMinBufferSize(
                44100,//采样频率
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,//双声道
                AudioFormat.ENCODING_PCM_16BIT//采样深度
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )

            //这个 KEY_MAX_INPUT_SIZE 一定要设置，不然很容易指针越界。
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            starOutputData()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun starOutputData() {
        thread {
            audioRecord?.startRecording()
            //输入
            val buffer = ByteArray(minBufferSize)
            while (!isStopped) {
                providePCM(buffer)
            }
            inCanRelease = true
        }

        thread {
            //输出音数据
            val tempFile = Directory.createSDCardRootAppPath(Directory.createTempFileName(Directory.AUDIO_FORMAT_AAC))
            val fileWriter = FileWriter(tempFile)
            val mediaCodecInfo = MediaCodec.BufferInfo()
            while (!isStopped) {
                produceAAC(mediaCodecInfo, fileWriter)
            }
            fileWriter.closeSafely()
            outCanRelease = true
        }
    }

    private fun providePCM(buffer: ByteArray) {
        val audioRecord = this.audioRecord ?: return
        val mediaCodec = this.mediaCodec ?: return

        val len = audioRecord.read(buffer, 0, buffer.size)
        if (len <= 0) {
            return
        }

        val index = mediaCodec.dequeueInputBuffer(10000)
        if (index < 0) {
            return
        }

        val inputBuffer = mediaCodec.getInputBuffer(index) ?: return
        inputBuffer.clear()
        inputBuffer.put(buffer, 0, len)
        //nanoTime 方法只能用于测量已过的时间，与系统或钟表时间的其他任何时间概念无关。【这里只需要给一个精确的相对时间】
        val presentationTime = System.nanoTime() / 1000
        //Timber.d("input presentationTime = $presentationTime")
        mediaCodec.queueInputBuffer(index, 0, len, presentationTime, 0)
    }

    private fun produceAAC(bufferInfo: MediaCodec.BufferInfo, fileWriter: FileWriter) {
        val mediaCodec = this.mediaCodec ?: return
        val index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        if (index < 0) {
            return
        }

        //编码好的数据
        val outputBuffer = mediaCodec.getOutputBuffer(index) ?: return
        val format = mediaCodec.getOutputFormat(index)
        val outData = ByteArray(bufferInfo.size)
        outputBuffer.get(outData)

        if (startTime == 0L) {
            //记录开始时间
            startTime = bufferInfo.presentationTimeUs / 1000
            Timber.d("startTime = $startTime")
        }

        //减去其实时间，就得到一个递增的时间戳。
        val presentationTime = bufferInfo.presentationTimeUs / 1000 - startTime

        //for debug
        //Timber.d("output presentationTime = $presentationTime")
        if (!printed) {
            outputFormat(format, bufferInfo, presentationTime)
            printed = true
        }
        //FileUtils.writeContent(fileWriter, outData)

        //在使用 MediaCodec 将 PCM 压缩编码为 AAC 时，编码器输出的 AAC 是没有 ADTS 头的原始帧的。
        //但是有 Audio Specific Config，长度为两个字节，包含了采样率、声道数、采样深度三个信息。ADTS 中包含的信息更多。
        //第一个数据就是两个字节长度的 Audio Specific Config，在
        // https://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Audio_Specific_Config 中有描述这两个字节中包含的信息。

        //要分区 AAC 文件与 FLV 中的 AAC：
        //  1. 如果是希望将编码后的 AAC 保存到本地，需要将 Audio Specific Config 转换为 ADTS 头的原始帧，写在每一个音频帧之前。
        //  2. 如果是通过 LibRTMP 发送 AAC 音频包，遵循的是 FLV 的规范，不需要添加 ADTS 头的原始帧，只需要 Audio Specific Config 即可。
        val type = if (outData.size == 2) {
            Timber.d("received aac info.")
            TYPE_AUDIO_INFO
        } /*其余的数据则是实际的音频数据*/ else {
            TYPE_AUDIO_DATA
        }

        if (packetDataCallback != null) {
            packetDataCallback?.onPacket(
                Packet(outData, presentationTime, type)
            )
        }

        mediaCodec.releaseOutputBuffer(index, false)
    }

    fun stop() {
        isStopped = true
        //释放
        thread {
            while (true) {
                if (inCanRelease && outCanRelease) {
                    mediaCodec?.stop()
                    mediaCodec?.release()
                    audioRecord?.stop()
                    audioRecord?.release()
                    break
                }
                Thread.sleep(100)
            }
        }
    }

    private fun outputFormat(
        format: MediaFormat,
        bufferInfo: MediaCodec.BufferInfo,
        presentationTime: Long
    ) {
        Timber.d(
            "format = %s, raw presentationTime = %d, presentationTime = %d",
            format.toString(),
            bufferInfo.presentationTimeUs,
            presentationTime
        )
    }

}