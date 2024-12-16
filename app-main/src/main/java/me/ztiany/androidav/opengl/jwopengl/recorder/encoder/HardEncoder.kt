package me.ztiany.androidav.opengl.jwopengl.recorder.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import timber.log.Timber
import kotlin.concurrent.thread

class HardEncoder(
    private val path: String,
    speedValue: Float
) : AbstractEncoder() {

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private lateinit var inputSurface: Surface

    override val mode = EncoderMode.Hard

    @Volatile var ended = false

    private var lastTimeStamp: Long = 0
    private var track = 0
    private val speed = speedValue

    override fun init(width: Int, height: Int) {
        inputSurface = initMediaCodec(width, height)
    }

    override fun getInputSurfaceView(): Surface {
        return inputSurface
    }

    override fun start() {
        ended = false
        thread {
            codec()
        }
    }

    override fun stop() {
        ended = true
    }

    private fun initMediaCodec(width: Int, height: Int): Surface {
        //视频格式
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        //从 surface 当中获得
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        //码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3)
        //帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25)
        //关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)
        //创建编码器
        val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        //配置编码器
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        //输入数据
        val surface = mediaCodec.createInputSurface()
        this.mediaCodec = mediaCodec
        //开始编码
        mediaCodec.start()

        //混合器 (复用器) 将编码的 h.264 封装为 mp4
        mediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        return surface
    }

    private fun codec() {
        val mediaCodec = this.mediaCodec ?: return
        val mediaMuxer = this.mediaMuxer ?: return

        while (true) {
            if (ended) {
                endStream()
                break
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val index: Int = mediaCodec.dequeueOutputBuffer(bufferInfo, 3000)

            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!ended) {
                    continue
                } else {
                    endStream()
                    break
                }
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //输出格式发生改变 ，第一次总会调用所以在这里开启混合器
                val newFormat: MediaFormat = mediaCodec.outputFormat
                track = mediaMuxer.addTrack(newFormat)
                mediaMuxer.start()
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //可以忽略
            } else {

                //调整时间戳
                bufferInfo.presentationTimeUs = (bufferInfo.presentationTimeUs / speed).toLong()
                //有时候会出现异常 ： timestampUs xxx < lastTimestampUs yyy for Video track
                if (bufferInfo.presentationTimeUs <= lastTimeStamp) {
                    bufferInfo.presentationTimeUs = (lastTimeStamp + 1000000 / 25 / speed).toLong()
                }
                lastTimeStamp = bufferInfo.presentationTimeUs

                //正常则 index 获得缓冲区下标
                val encodedData = mediaCodec.getOutputBuffer(index) ?: continue

                //如果当前的 buffer是配置信息，不管它
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    //设置从哪里开始读数据(读出来就是编码后的数据)
                    encodedData.position(bufferInfo.offset)
                    //设置能读数据的总长度
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    //写出为mp4
                    mediaMuxer.writeSampleData(track, encodedData, bufferInfo)
                }

                // 释放这个缓冲区，后续可以存放新的编码后的数据啦
                mediaCodec.releaseOutputBuffer(index, false)

                // 如果给了结束信号 signalEndOfInputStream
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            }
        }
    }

    private fun endStream() {
        mediaCodec?.run {
            signalEndOfInputStream()
            stop()
            release()
        }
        mediaMuxer?.run {
            stop()
            release()
        }
        Timber.d("stop recording 1.")
    }

}