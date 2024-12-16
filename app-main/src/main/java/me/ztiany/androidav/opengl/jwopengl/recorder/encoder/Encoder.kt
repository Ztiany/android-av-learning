package me.ztiany.androidav.opengl.jwopengl.recorder.encoder

import android.view.Surface

interface Encoder {

    val mode: EncoderMode

    fun init(width: Int, height: Int)

    fun start()

    fun stop()

    ///////////////////////////////////////////////////////////////////////////
    // Hard
    ///////////////////////////////////////////////////////////////////////////

    fun getInputSurfaceView(): Surface

    ///////////////////////////////////////////////////////////////////////////
    // Soft
    ///////////////////////////////////////////////////////////////////////////

    fun onFrame()

}

abstract class AbstractEncoder : Encoder {

    override fun getInputSurfaceView(): Surface {
        throw UnsupportedOperationException("not implemented.")
    }

    override fun onFrame() {

    }

}

enum class EncoderMode {
    Hard, Soft
}