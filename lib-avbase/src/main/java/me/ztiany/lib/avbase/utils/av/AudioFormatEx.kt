@file:JvmName("AudioFormatEx")

package me.ztiany.lib.avbase.utils.av

import android.media.AudioFormat

/** the params [channelConfig] is like [AudioFormat.CHANNEL_IN_MONO].*/
fun getChannelCount(channelConfig: Int): Int {
    @Suppress("DUPLICATE_LABEL_IN_WHEN")
    return when (channelConfig) {
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_OUT_MONO -> 1
        AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_OUT_STEREO -> 2
        else -> throw IllegalArgumentException("getChannelCount received: $channelConfig")
    }
}

fun getChannelOutConfig(channelCount: Int): Int {
    return when (channelCount) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> throw IllegalArgumentException("getChannelConfig received: $channelCount")
    }
}

/** the params [audioEncoding] is like [AudioFormat.ENCODING_PCM_16BIT].*/
fun getBitsPerSample(audioEncoding: Int): Int {
    return when (audioEncoding) {
        AudioFormat.ENCODING_PCM_8BIT -> 8
        AudioFormat.ENCODING_PCM_16BIT -> 16
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
        AudioFormat.ENCODING_PCM_32BIT -> 32
        else -> throw IllegalArgumentException("getBitsPerSample received: $audioEncoding")
    }
}

fun getAudioEncoding(bitsPerSample: Int): Int {
    return when (bitsPerSample) {
        8 -> AudioFormat.ENCODING_PCM_8BIT
        16 -> AudioFormat.ENCODING_PCM_16BIT
        24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
        32 -> AudioFormat.ENCODING_PCM_32BIT
        else -> throw IllegalArgumentException("getAudioEncoding received: $bitsPerSample")
    }
}