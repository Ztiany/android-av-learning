package me.ztiany.androidav.opengl.jwopengl.common

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.View
import timber.log.Timber

class TextureViewProvider(private val textureView: TextureView) : SurfaceProvider {

    private lateinit var surfaceProviderCallback: SurfaceProviderCallback
    private var surface: Surface? = null

    override fun start(surfaceProviderCallback: SurfaceProviderCallback) {
        if (this::surfaceProviderCallback.isInitialized) {
            throw UnsupportedOperationException("call this only once.")
        }
        this.surfaceProviderCallback = surfaceProviderCallback

        textureView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {

            override fun onViewAttachedToWindow(v: View) {
                Timber.d("onViewAttachedToWindow")
            }

            override fun onViewDetachedFromWindow(v: View) {
                Timber.d("onViewDetachedFromWindow")
            }
        })

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Timber.d("onSurfaceTextureAvailable $surface")
                this@TextureViewProvider.surface = Surface(surface)
                this@TextureViewProvider.surface?.let {
                    surfaceProviderCallback.onSurfaceAvailable(it)
                    surfaceProviderCallback.onSurfaceChanged(it, width, height)
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Timber.d("onSurfaceTextureSizeChanged $surface")
                this@TextureViewProvider.surface?.let {
                    surfaceProviderCallback.onSurfaceChanged(it, width, height)
                }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Timber.d("onSurfaceTextureDestroyed $surface")
                surfaceProviderCallback.onSurfaceDestroyed()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

        }
    }

    override fun stop() {

    }

}