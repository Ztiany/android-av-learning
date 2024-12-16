package me.ztiany.androidav.opengl.jwopengl

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.androidav.opengl.jwopengl.common.GLController
import me.ztiany.androidav.opengl.jwopengl.common.GLParams
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import me.ztiany.androidav.opengl.jwopengl.renderer.*
import me.ztiany.androidav.opengl.jwopengl.usecase.JavaWithOpenGLCommonActivity
import me.ztiany.androidav.opengl.jwopengl.usecase.camera.EGLCameraPreviewWithActivity
import me.ztiany.androidav.opengl.jwopengl.usecase.camera.OpenGLCameraPreviewActivity
import me.ztiany.androidav.opengl.jwopengl.usecase.recorder.OpenGLRecorderActivity
import me.ztiany.lib.avbase.utils.ui.IEntrance
import me.ztiany.lib.avbase.utils.ui.buildLayoutEntrance

class JavaWithOpenGLMainActivity : AppCompatActivity() {

    private data class CommonItem(
        override val title: String,
        val renderer: Class<out GLRenderer>,
        val controller: Class<out GLController>? = null,
        val params: GLParams? = null
    ) : IEntrance

    private data class ActivityItem(
        override val title: String,
        val activity: Class<out Activity>,
        val onIntent: ((Intent) -> Unit)? = null,
    ) : IEntrance

    private val entrances = listOf(
        CommonItem("绘制背景", BackgroundRenderer::class.java),
        CommonItem("绘制三角形", TriangleRenderer::class.java),
        CommonItem("绘制渐变矩形", GradualRectangleRenderer::class.java),
        CommonItem("绘制纹理", TextureRenderer::class.java),
        CommonItem("绘制纹理（修正1）", Fixed1TextureRenderer::class.java),
        CommonItem("绘制纹理（修正2）", Fixed2TextureRenderer::class.java),
        ActivityItem("相机预览（GLSurfaceView）", OpenGLCameraPreviewActivity::class.java),
        ActivityItem("相机预览（EGL + SurfaceView）", EGLCameraPreviewWithActivity::class.java) {
            it.putExtra(EGLCameraPreviewWithActivity.RENDER_TYPE, EGLCameraPreviewWithActivity.WITH_SURFACE_VIEW)
        },
        ActivityItem("相机预览（EGL + TextureView）", EGLCameraPreviewWithActivity::class.java) {
            it.putExtra(EGLCameraPreviewWithActivity.RENDER_TYPE, EGLCameraPreviewWithActivity.WITH_TEXTURE_VIEW)
        },
        ActivityItem("相机预览+特效+录频（FBO）", OpenGLRecorderActivity::class.java),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            buildLayoutEntrance(
                this,
                entrances
            ) { _, index ->
                handleClicked(index)
            }
        )
    }

    private fun handleClicked(index: Int) {
        val item = entrances[index]
        if (item is CommonItem) {
            JavaWithOpenGLCommonActivity.start(this, item.title, item.renderer)
        } else if (item is ActivityItem) {
            startActivity(Intent(this, item.activity).apply {
                item.onIntent?.invoke(this)
            })
        }
    }

}