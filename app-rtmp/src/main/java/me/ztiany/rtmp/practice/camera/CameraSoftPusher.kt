package me.ztiany.rtmp.practice.camera

import android.hardware.camera2.CameraDevice
import android.util.Size
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.lib.avbase.utils.av.YUVUtils
import me.ztiany.rtmp.common.Pusher
import me.ztiany.rtmp.common.RtmpPusher
import me.ztiany.rtmp.common.RtmpPusher.VIDEO_TYPE_YUV
import java.util.concurrent.Executors

class CameraSoftPusher(
    private val cameraId: String,
    context: AppCompatActivity,
    textureView: TextureView
) : Pusher {

    private val camera2VideoSource = Camera2VideoSource(context, textureView)

    private val rtmpPusher = RtmpPusher.getInstance()
    private var i420: ByteArray? = null

    @Volatile private var rtmpInitSucceeded = false

    private val executor = Executors.newSingleThreadExecutor()

    private val rtmpCallback = object : RtmpPusher.Callback {
        override fun onInitFailed() = Unit
        override fun onInitSuccess() {
            rtmpInitSucceeded = true
        }

        override fun onSendError() = Unit
    }

    private val camera2Listener = object : Camera2Listener {

        override fun onCameraOpened(
            cameraDevice: CameraDevice,
            cameraId: String,
            previewSize: Size,
            displayOrientation: Int,
            isMirror: Boolean
        ) {
            if ((displayOrientation / 90).mod(2) == 1) {//竖屏
                rtmpPusher.initVideoCodec(previewSize.height, previewSize.width, 15, 800_000, RtmpPusher.VIDEO_FORMAT_I420)
            } else {//横屏
                rtmpPusher.initVideoCodec(previewSize.width, previewSize.height, 15, 800_000, RtmpPusher.VIDEO_FORMAT_I420)
            }
        }

        override fun onPreview(y: ByteArray, u: ByteArray, v: ByteArray, previewSize: Size, stride: Int) {
            if (!rtmpInitSucceeded) {
                return
            }

            var i420buffer = i420
            if (i420buffer == null || i420buffer.size != previewSize.width * previewSize.height * 3 / 2) {
                i420buffer = ByteArray(previewSize.width * previewSize.height * 3 / 2)
            }

            YUVUtils.i420FromYUVCutToWidth(y, u, v, i420buffer, stride, previewSize.width, previewSize.height)

            val i420bufferRotated = ByteArray(previewSize.width * previewSize.height * 3 / 2)
            YUVUtils.i420Rotate90CW(i420buffer, i420bufferRotated, previewSize.width, previewSize.height)

            executor.execute {
                rtmpPusher.sendVideoPacket(i420bufferRotated, VIDEO_TYPE_YUV, 0L)
            }
        }

        override fun onCameraClosed() {

        }

        override fun onCameraError(e: Exception?) {
        }

    }

    override fun start(url: String) {
        rtmpPusher.init()
        rtmpPusher.setCallback(rtmpCallback)
        rtmpPusher.start(url)
        camera2VideoSource.start(640, 480, cameraId, camera2Listener)
    }

    override fun pause() {
        camera2VideoSource.pause()
    }

    override fun resume() {
        camera2VideoSource.resume()
    }

    override fun stop() {
        rtmpInitSucceeded = false
        camera2VideoSource.stop()
        rtmpPusher.stop()
        rtmpPusher.release()
        rtmpPusher.setCallback(null)
    }

}