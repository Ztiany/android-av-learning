package me.ztiany.androidav.opengl.jwopengl.usecase.recorder

import android.graphics.Point
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.core.view.doOnLayout
import me.ztiany.androidav.databinding.OpenglActivityRecorderBinding
import me.ztiany.androidav.opengl.jwopengl.recorder.RecorderManager
import me.ztiany.androidav.opengl.jwopengl.recorder.Speed
import me.ztiany.androidav.opengl.jwopengl.recorder.filter.*
import me.ztiany.androidav.opengl.jwopengl.recorder.getSpeedName
import me.ztiany.androidav.opengl.oglcamera.CameraBuilder
import me.ztiany.androidav.opengl.oglcamera.CameraListener
import me.ztiany.androidav.opengl.oglcamera.CameraOperator
import me.ztiany.lib.avbase.app.activity.BaseActivity
import me.ztiany.lib.avbase.utils.Directory

/**RENDERMODE_WHEN_DIRTY 不需要调用 surface 的 onResume/onPause */
class OpenGLRecorderActivity : BaseActivity<OpenglActivityRecorderBinding>() {

    private var cameraOperator: CameraOperator? = null
    private val recorderManager = RecorderManager()
    private var storePath: String = ""
    private var currentSpeed = Speed.MODE_NORMAL

    private val allEffect = listOf(
        "灵魂出窍" to EffectSoulFilter(),
        "屏幕二分" to EffectSplit2Filter(),
        "屏幕三分" to EffectSplit3Filter(),
        "美颜 1" to BeautifyFilter(1),
        "美颜 2" to BeautifyFilter(2),
        "无特效" to null
    )

    private var currentIndex = -1

    private fun nextEffect(): Pair<String, GLFilter?> {
        currentIndex++
        if (currentIndex >= allEffect.size) {
            currentIndex = 0
        }
        return allEffect[currentIndex]
    }

    private fun onCameraAvailable(previewSize: Size, displayOrientation: Int, isFront: Boolean) {
        recorderManager.onCameraAvailable(previewSize, displayOrientation, isFront) {
            cameraOperator?.startPreview(it)
        }
    }

    override fun setUpLayout(savedInstanceState: Bundle?) {
        recorderManager.init(binding.openglCameraView)
        setUpRecordBtnLogic()
        setUpSpeedBtnLogic()
        setUpEffectBtnLogic()
        binding.openglBtnSwitch.setOnClickListener {
            cameraOperator?.switchCamera()
        }
        window.decorView.doOnLayout {
            setUpCamera()
        }
    }

    private fun setUpEffectBtnLogic() {
        binding.openglBtnEffect.setOnClickListener {
            val (name, filter) = nextEffect()
            binding.openglBtnEffect.text = name
            recorderManager.removeAllEffect()
            if (filter != null) {
                recorderManager.addEffect(filter)
            }
        }
    }

    private fun setUpSpeedBtnLogic() {
        recorderManager.speed = currentSpeed
        binding.openglBtnSpeed.text = currentSpeed.getSpeedName()
        binding.openglBtnSpeed.setOnClickListener {
            var next = currentSpeed.ordinal + 1
            if (next > Speed.MODE_EXTRA_FAST.ordinal) {
                next = 0
            }
            currentSpeed = Speed.values()[next]
            binding.openglBtnSpeed.text = currentSpeed.getSpeedName()
            recorderManager.speed = currentSpeed
        }
    }

    private fun setUpRecordBtnLogic() {
        binding.openglBtnStart.setOnClickListener {
            if (recorderManager.isRecording) {
                recorderManager.stopRecording()
                Toast.makeText(this, "视频保存到了 $storePath", Toast.LENGTH_LONG).show()
                binding.openglBtnStart.text = "开始"
            } else {
                storePath = Directory.createSDCardRootAppTimeNamingPath(Directory.VIDEO_FORMAT_MP4).absolutePath
                recorderManager.startRecording(storePath)
                binding.openglBtnStart.text = "停止"
            }

            binding.openglBtnSwitch.isEnabled = !recorderManager.isRecording
            binding.openglBtnSpeed.isEnabled = !recorderManager.isRecording
            binding.openglBtnEffect.isEnabled = !recorderManager.isRecording
        }
    }

    private fun setUpCamera() {
        val cameraListener = CameraListener { _, cameraID, previewSize, displayOrientation ->
            onCameraAvailable(previewSize, displayOrientation, CameraOperator.CAMERA_ID_FRONT == cameraID)
        }

        val openglCameraView = binding.openglCameraView

        cameraOperator = CameraBuilder()
            .cameraListener(cameraListener)
            .maxPreviewSize(Point(1920, 1080))
            .minPreviewSize(Point(100, 100))
            .previewViewSize(Point(openglCameraView.width, openglCameraView.height))
            .targetPreviewSize(Point(800, 480))
            .specificCameraId(CameraOperator.CAMERA_ID_BACK)
            .context(applicationContext)
            .rotation(windowManager.defaultDisplay.rotation)
            .build("2")
            .also {
                it.start()
            }
    }

    override fun onResume() {
        super.onResume()
        cameraOperator?.start()
    }

    override fun onPause() {
        super.onPause()
        cameraOperator?.stop()
        recorderManager.stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraOperator?.release()
    }

}