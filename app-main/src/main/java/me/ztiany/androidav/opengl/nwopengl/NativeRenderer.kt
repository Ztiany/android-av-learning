package me.ztiany.androidav.opengl.nwopengl

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class NativeRenderer(
    type: Int,
    context: Context
) : GLSurfaceView.Renderer {

    init {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    destroy()
                }
            })
        } else {
            Timber.w("The provided context is not a LifecycleOwner, call destroy on your own.")
        }
    }

    private val handle = AtomicLong(createNativeRenderer(type))
    private val handleBackup = handle.get()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        if (handle.get() != 0L) {
            onSurfaceCreated(handle.get())
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        if (handle.get() != 0L) {
            onViewportChanged(handle.get(), width, height)
        }
    }

    override fun onDrawFrame(gl: GL10) {
        if (handle.get() != 0L) {
            onDrawFrame(handle.get())
        }
    }

    fun destroy() {
        if (handle.compareAndSet(handleBackup, 0)) {
            onSurfaceDestroy(handleBackup)
        }
    }

    private external fun createNativeRenderer(type: Int): Long

    private external fun onSurfaceCreated(handle: Long)

    private external fun onViewportChanged(handle: Long, width: Int, height: Int)

    private external fun onDrawFrame(handle: Long)

    private external fun onSurfaceDestroy(handle: Long)

}