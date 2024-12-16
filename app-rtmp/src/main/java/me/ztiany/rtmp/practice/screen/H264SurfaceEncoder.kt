package me.ztiany.rtmp.practice.screen

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.view.Surface
import me.ztiany.lib.avbase.utils.Directory
import me.ztiany.lib.avbase.utils.closeSafely
import me.ztiany.rtmp.common.Packet
import me.ztiany.rtmp.common.PacketDataCallback
import me.ztiany.rtmp.common.TYPE_VIDEO_DATA
import timber.log.Timber
import java.io.FileWriter
import kotlin.concurrent.thread

class H264SurfaceEncoder {

    var packetDataCallback: PacketDataCallback? = null

    private var mediaCodec: MediaCodec? = null

    @Volatile private var isStopped = true

    private var timeStamp = 0L
    private var startTime = 0L

    fun initEncoder(videoConfig: VideoConfig): Boolean {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            videoConfig.targetWidth,
            videoConfig.targetHeight
        )

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 400000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3)

        return try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
                configure(
                    format,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE
                )
                val surface: Surface = createInputSurface()
                videoConfig.surface = surface
            }
            true
        } catch (e: Exception) {
            false
        }

    }

    fun start() {
        isStopped = false
        val codec = mediaCodec ?: return
        codec.start()
        thread {
            startEncoding(codec)
            startTime = 0
            codec.stop()
            codec.release()
        }
    }

    private fun startEncoding(codec: MediaCodec) {
        val tempFile = Directory.createSDCardRootAppPath(Directory.createTempFileName(Directory.VIDEO_FORMAT_H264))
        val fileWriter = FileWriter(tempFile)
        var printed = false

        while (!isStopped) {
            val bufferInfo = MediaCodec.BufferInfo()

            //2000毫秒，手动触发输出关键帧【防止画面静止而长时间没有 I 帧】
            if (System.currentTimeMillis() - timeStamp >= 5000) {
                val params = Bundle()
                //立即刷新，让下一帧是关键帧
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                codec.setParameters(params)
                timeStamp = System.currentTimeMillis()
            }

            val index = codec.dequeueOutputBuffer(bufferInfo, 10000L)

            if (isStopped) {
                //停止了，不需要使用 index
                //codec.releaseOutputBuffer(index, false)
                return
            }

            if (index >= 0) {
                val byteBuffer = codec.getOutputBuffer(index) ?: continue
                val format = codec.getOutputFormat(index)
                val data = ByteArray(bufferInfo.size)
                byteBuffer.get(data)
                if (startTime == 0L) {
                    //记录开始时间，微妙-->毫秒
                    startTime = bufferInfo.presentationTimeUs / 1000
                }

                val presentationTime = (bufferInfo.presentationTimeUs / 1000) - startTime

                //for debug.
                //FileUtils.writeContent(fileWriter, data)
                if (!printed) {
                    outputFormat(format, bufferInfo, presentationTime)
                    printed = true
                }

                if (!isStopped) {
                    packetDataCallback?.onPacket(
                        Packet(data, presentationTime, TYPE_VIDEO_DATA)
                    )
                }

                codec.releaseOutputBuffer(index, false)
            }

        }

        fileWriter.closeSafely()
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

    fun stop() {
        isStopped = true
    }

}