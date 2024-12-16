package me.ztiany.androidav.opengl.nwopengl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import me.ztiany.androidav.databinding.OpenglActivityNativeCommonBinding
import me.ztiany.lib.avbase.app.activity.BaseActivity

class NativeWithOpenGLCommonActivity : BaseActivity<OpenglActivityNativeCommonBinding>() {

    private lateinit var nativeRenderer: NativeRenderer

    override fun setUpLayout(savedInstanceState: Bundle?) {
        showTitle()
        initGlSurfaceView()
    }

    private fun initGlSurfaceView() {
        with(binding.openglGlSurfaceView) {
            setEGLContextClientVersion(2)
            nativeRenderer = NativeRenderer(intent.getIntExtra(KEY_RENDER_TYPE, 0), this@NativeWithOpenGLCommonActivity)
            setRenderer(nativeRenderer)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.openglGlSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.openglGlSurfaceView.onResume()
    }

    private fun showTitle() {
        val title = intent.getStringExtra(KEY_TITLE)
        supportActionBar?.title = title
    }

    companion object {

        private const val KEY_TITLE = "key_title"
        private const val KEY_RENDER_TYPE = "render_type_key"

        fun start(
            context: Context,
            title: String,
            type: Int,
        ) {
            with(Intent(context, NativeWithOpenGLCommonActivity::class.java)) {
                putExtra(KEY_TITLE, title)
                putExtra(KEY_RENDER_TYPE, type)
                context.startActivity(this)
            }
        }

    }

}