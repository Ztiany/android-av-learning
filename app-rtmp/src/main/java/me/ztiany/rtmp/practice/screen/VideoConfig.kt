package me.ztiany.rtmp.practice.screen

import android.view.Surface

data class VideoConfig(
    val targetWidth: Int,
    val targetHeight: Int,
    var surface: Surface? = null
)