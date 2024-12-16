package me.ztiany.androidav.player.mediacodec

import android.view.Surface

interface SurfaceRenderer {

    fun provideSurface(): Surface

}