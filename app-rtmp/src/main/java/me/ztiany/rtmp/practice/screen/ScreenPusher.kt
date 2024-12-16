package me.ztiany.rtmp.practice.screen

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ScreenUtils
import me.ztiany.rtmp.audio.AACAudioSource
import me.ztiany.rtmp.common.*
import kotlin.math.roundToInt

class ScreenPusher(
    private val context: AppCompatActivity
) : Pusher {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection

    private val screenVideoSource = ScreenVideoSource()
    private val h264SurfaceEncoder = H264SurfaceEncoder()

    private val aacAudioSource = AACAudioSource()

    private val rtmpPusher = RtmpPusher.getInstance()
    private var url = ""

    private val videoConfig = VideoConfig(
        (ScreenUtils.getScreenWidth() * 0.5F).roundToInt(),
        (ScreenUtils.getScreenHeight() * 0.5F).roundToInt(),
    )

    private val videoPacketCallback = object : PacketDataCallback {
        override fun onPacket(packet: Packet) {
            sendVideoPacket(packet)
        }
    }
    private val audioPacketCallback = object : PacketDataCallback {
        override fun onPacket(packet: Packet) {
            sendAudioPacket(packet)
        }
    }

    private val rtmpCallback = object : RtmpPusher.Callback {
        override fun onInitFailed() = Unit
        override fun onInitSuccess() {
            startScreenLiveHard(mediaProjection)
        }

        override fun onSendError() = Unit
    }

    init {
        rtmpPusher.setCallback(rtmpCallback)
    }

    override fun start(url: String) {
        this.url = url
        initScreenProjection()
    }

    private fun initScreenProjection() {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        context.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_HARD)
        h264SurfaceEncoder.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            if (REQUEST_CODE_SCREEN_HARD == requestCode) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                initPusher()
            }
        }
    }

    private fun initPusher() {
        rtmpPusher.init()
        rtmpPusher.start(url)
    }

    private fun startScreenLiveHard(mediaProjection: MediaProjection) {
        aacAudioSource.packetDataCallback = audioPacketCallback
        aacAudioSource.start()

        h264SurfaceEncoder.packetDataCallback = videoPacketCallback
        h264SurfaceEncoder.initEncoder(videoConfig)
        screenVideoSource.start(mediaProjection, videoConfig)
        h264SurfaceEncoder.start()
    }

    private fun sendVideoPacket(packet: Packet) {
        rtmpPusher.sendVideoPacket(
            packet.byteArray,
            RtmpPusher.VIDEO_TYPE_X264,
            packet.presentationTime
        )
    }

    private fun sendAudioPacket(packet: Packet) {
        if (packet.type == TYPE_AUDIO_INFO) {
            rtmpPusher.sendAudioPacket(
                packet.byteArray,
                RtmpPusher.AUDIO_TYPE_AAC_INFO,
                0L
            )
        } else {
            rtmpPusher.sendAudioPacket(
                packet.byteArray,
                RtmpPusher.AUDIO_TYPE_AAC_DATA,
                packet.presentationTime
            )
        }
    }

    override fun stop() {
        rtmpPusher.stop()
        rtmpPusher.release()
        rtmpPusher.setCallback(null)
        aacAudioSource.stop()
        screenVideoSource.stop()
        h264SurfaceEncoder.stop()
    }

    companion object {
        private const val REQUEST_CODE_SCREEN_HARD = 100
    }

}