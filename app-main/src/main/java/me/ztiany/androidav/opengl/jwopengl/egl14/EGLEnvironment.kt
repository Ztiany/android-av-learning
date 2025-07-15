package me.ztiany.androidav.opengl.jwopengl.egl14

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.view.Surface
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import me.ztiany.androidav.opengl.jwopengl.common.RenderMode
import me.ztiany.androidav.opengl.jwopengl.common.SurfaceProvider
import me.ztiany.androidav.opengl.jwopengl.common.SurfaceProviderCallback
import timber.log.Timber

class EGLEnvironment(
    private val surfaceProvider: SurfaceProvider,
    private val eglAttribute: EGLAttribute
) {

    companion object {
        private const val MSG_EGL_INIT = 6
        private const val MSG_EGL_NEW_SURFACE = 2
        private const val MSG_EGL_SURFACE_REFRESH = 3
        private const val MSG_EGL_SURFACE_DESTROYED = 4
        private const val MSG_EGL_SURFACE_RELEASE = 5

        private const val MSG_RENDERER_SURFACE_CREATED = 105
        private const val MSG_RENDERER_SURFACE_CHANGED = 106
        private const val MSG_RENDERER_DRAW = 107
    }

    private val eglThread = HandlerThread("EGLEnvironment")

    private val eglCore = EGLCore()

    private lateinit var glRenderer: GLRenderer
    private lateinit var eglHandler: Handler

    @Volatile private var surfaceAvailable = false

    @Volatile var renderMode = RenderMode.Continuously

    fun start(GLRenderer: GLRenderer) {
        if (this::glRenderer.isInitialized) {
            throw IllegalStateException("renderer has already been set.")
        }

        this.glRenderer = GLRenderer

        eglThread.start()
        eglHandler = Handler(eglThread.looper, ::handleMessage)
        eglHandler.sendEmptyMessage(MSG_EGL_INIT)

        surfaceProvider.start(surfaceProviderCallback)
    }

    private val surfaceProviderCallback = object : SurfaceProviderCallback {
        override fun onSurfaceAvailable(surface: Surface) {
            surfaceAvailable = true
            eglHandler.sendMessage(Message.obtain().apply {
                what = MSG_EGL_NEW_SURFACE
                obj = surface
            })
        }

        override fun onSurfaceChanged(surface: Surface, width: Int, height: Int) {
            eglHandler.sendMessage(Message.obtain().apply {
                what = MSG_EGL_SURFACE_REFRESH
                arg1 = width
                arg2 = height
            })
        }

        override fun onSurfaceDestroyed() {
            surfaceAvailable = false
            eglHandler.removeCallbacksAndMessages(null)
            eglHandler.sendEmptyMessage(MSG_EGL_SURFACE_DESTROYED)
        }
    }

    fun release() {
        surfaceProvider.stop()
        eglHandler.removeCallbacksAndMessages(null)
        //A TextureView's onDestroy is called after an Activity's onDestroy.
        if (surfaceAvailable) {
            eglHandler.sendEmptyMessage(MSG_EGL_SURFACE_DESTROYED)
        }
        eglHandler.sendEmptyMessage(MSG_EGL_SURFACE_RELEASE)
        eglThread.quitSafely()
    }

    private fun handleMessage(message: Message): Boolean {
        if (message.what <= 100) {
            handleEGLMessage(message)
        } else {
            handleRendererMessage(message)
        }
        return true
    }

    private fun handleEGLMessage(message: Message) {
        when (message.what) {
            MSG_EGL_INIT -> {
                Timber.d("handleEGLMessage MSG_EGL_INIT")
                eglCore.makeEglContext(eglAttribute.sharedContext)
            }
            MSG_EGL_NEW_SURFACE -> {
                Timber.d("handleEGLMessage MSG_EGL_NEW_SURFACE")
                eglCore.makeEglWindowSurface(message.obj as Surface)
                eglCore.makeCurrent()
                eglHandler.sendEmptyMessage(MSG_RENDERER_SURFACE_CREATED)
            }
            MSG_EGL_SURFACE_REFRESH -> {
                Timber.d("handleEGLMessage MSG_EGL_SURFACE_REFRESH")
                //egl doesn't need to do anything.
                //...
                //notify renderer the surface size.
                eglHandler.sendMessage(Message.obtain().apply {
                    what = MSG_RENDERER_SURFACE_CHANGED
                    arg1 = message.arg1
                    arg2 = message.arg2
                })
                if (renderMode == RenderMode.Continuously) {
                    eglHandler.sendEmptyMessage(MSG_RENDERER_DRAW)
                }
            }
            MSG_EGL_SURFACE_DESTROYED -> {
                Timber.d("handleEGLMessage MSG_EGL_SURFACE_DESTROYED")
                glRenderer.onSurfaceDestroy()
                eglCore.destroySurface()
            }
            MSG_EGL_SURFACE_RELEASE -> {
                Timber.d("handleEGLMessage MSG_EGL_SURFACE_RELEASE")
                eglCore.release()
            }
        }
    }

    private fun handleRendererMessage(message: Message) {
        when (message.what) {
            MSG_RENDERER_SURFACE_CREATED -> {
                Timber.d("handleEGLMessage MSG_RENDERER_SURFACE_CREATED")
                if (eglCore.isActive() && surfaceAvailable) {
                    glRenderer.onSurfaceCreated()
                }
            }
            MSG_RENDERER_SURFACE_CHANGED -> {
                Timber.d("handleEGLMessage MSG_RENDERER_SURFACE_CHANGED")
                if (eglCore.isActive() && surfaceAvailable) {
                    glRenderer.onSurfaceChanged(message.arg1, message.arg2)
                }
            }
            MSG_RENDERER_DRAW -> {
                if (eglCore.isActive() && surfaceAvailable) {
                    glRenderer.onDrawFrame(message.obj)
                    eglCore.swapBuffers()
                }
                checkIfDrawContinuously()
            }
        }
    }

    /* TODO: optimize the delay time. */
    private fun checkIfDrawContinuously() {
        if (renderMode == RenderMode.Continuously) {
            eglHandler.sendEmptyMessageDelayed(MSG_RENDERER_DRAW, 16)
        }
    }

    fun requestRender(attach: Any? = null) {
        if (surfaceAvailable && eglCore.isActive()) {
            eglHandler.sendMessage(Message.obtain().apply {
                what = MSG_RENDERER_DRAW
                obj = attach
            })
        }
    }

    fun setPresentationTime(nanoseconds: Long) {
        eglCore.setPresentationTime(nanoseconds)
    }

}