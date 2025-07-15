package me.ztiany.androidav.opengl.jwopengl.common

/** All the methods in this class will be called in a OpenGL Marked Thread. */
interface GLRenderer {

    /**
     * Called when the surface is created or recreated.
     */
    fun onSurfaceCreated()

    /**
     * Called when the surface changed size.
     */
    fun onSurfaceChanged(width: Int, height: Int)

    /**
     * Called to draw the current frame.
     */
    fun onDrawFrame(attachment: Any? = null)

    /**
     * Called when egl context release.【It is not guaranteed that this method will be called at the end of drawing.】
     */
    fun onSurfaceDestroy()

}