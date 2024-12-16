package me.ztiany.androidav.opengl.jwopengl.egl14

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLExt
import android.view.Surface
import timber.log.Timber

class EGLCore {

    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    @Volatile private var hasSetUPCurrent = false

    fun makeEglContext(sharedContext: EGLContext? = null) {
        Timber.d("Thread = ${Thread.currentThread()}")
        //创建 display
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            throw   IllegalStateException("call makeEglContext() just once.")
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw   IllegalStateException("eglGetDisplay failed")
        } else {
            Timber.d("eglGetDisplay succeeded")
        }

        //初始化 display
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw   IllegalStateException("eglInitialize failed");
        } else {
            Timber.d("eglInitialize succeeded")
        }

        //配置 egl 的属性，格式：[key, value, key, value, ... , EGL14.EGL_NONE]，必须以 EGL14.EGL_NONE 结尾
        val eglConfigAttributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8, //颜色缓冲区中红色位数
            EGL14.EGL_GREEN_SIZE, 8, //颜色缓冲区中绿色位数
            EGL14.EGL_BLUE_SIZE, 8, //颜色缓冲区中蓝色位数
            EGL14.EGL_ALPHA_SIZE, 8, //颜色缓冲区中透明度位数
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //掩码，配置要支持的渲染接口，这里是 OpenGL ES 2.0
            EGL14.EGL_NONE//以 EGL_NONE 结尾
        )

        val numConfigs = IntArray(1)
        val eglConfigs = arrayOfNulls<EGLConfig>(1)
        if (!EGL14.eglChooseConfig(eglDisplay, eglConfigAttributes, 0, eglConfigs, 0, eglConfigs.size, numConfigs, 0) || eglConfigs[0] == null) {
            throw   IllegalStateException("eglChooseConfig failed")
        } else {
            eglConfig = eglConfigs[0]
            Timber.d("eglChooseConfig succeeded")
        }

        //创建 Context
        val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        try {
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, sharedContext ?: EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
        } catch (t: Throwable) {
            Timber.e(t)
        }
        if (eglConfig == EGL14.EGL_NO_CONTEXT) {
            throw   IllegalStateException("eglCreateContext failed")
        } else {
            eglConfig = eglConfigs[0]
            Timber.d("eglCreateContext succeeded")
        }
    }

    /**
     * 创建可显示的渲染缓存。
     *
     * @param surface 本地渲染窗口的 surface
     */
    fun makeEglWindowSurface(surface: Surface) {
        checkSurfaceExisted()
        val surfaceAttributes = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttributes, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw   IllegalStateException("eglCreateContext failed")
        }
    }

    /**
     * 创建离屏渲染缓存
     *
     * @param width 缓存窗口宽
     * @param height 缓存窗口高
     */
    fun createOffScreenSurface(width: Int, height: Int) {
        checkSurfaceExisted()
        val surfaceAttr = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttr, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw   IllegalStateException("eglCreatePbufferSurface failed")
        }
    }

    private fun checkSurfaceExisted() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            throw UnsupportedOperationException("release the previous surface first.")
        }
    }

    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw   IllegalStateException("eglCreateContext failed")
        }
        hasSetUPCurrent = true
    }

    /**
     * 将缓存图像数据发送到设备进行显示
     */
    fun swapBuffers(): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * 设置当前帧的时间，单位：纳秒。<br/>
     *
     *函数说明：specifies the time at which the current color buffer of <surface> should be presented to the viewer.The <time> parameter should be a time in
     * nanoseconds, but the exact meaning of the time depends on the native window system's use of the presentation time.  In situations where an absolute time
     * is needed such as displaying the color buffer on a display device, the time should correspond to the system monotonic up-time clock.  For situations in which
     * an absolute time is not needed such as using the color buffer for video encoding, the presentation time of the first frame may be arbitrarily chosen and
     * those of subsequent frames chosen relative to that of the first frame. 具体参考 [官方文档](https://www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_presentation_time.txt)。
     */
    fun setPresentationTime(nanoseconds: Long) {
        Timber.d("nanoseconds = $nanoseconds")
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nanoseconds)
    }

    /**
     * 销毁 EGLSurface，并解除上下文绑定。
     */
    fun destroySurface() {
        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
        hasSetUPCurrent = false
    }

    /**
     * 释放资源
     */
    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay. So for every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
        eglConfig = null
        hasSetUPCurrent = false
    }

    fun isActive(): Boolean {
        return eglDisplay != EGL14.EGL_NO_DISPLAY
                && eglContext != EGL14.EGL_NO_CONTEXT
                && eglSurface != EGL14.EGL_NO_SURFACE
                && eglConfig != null
                && hasSetUPCurrent
    }

}