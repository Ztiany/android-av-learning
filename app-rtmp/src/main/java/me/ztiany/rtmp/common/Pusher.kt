package me.ztiany.rtmp.common

import android.content.Intent

interface Pusher {

    fun start(url: String)

    fun stop()

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    fun resume() {

    }

    fun pause() {

    }

}