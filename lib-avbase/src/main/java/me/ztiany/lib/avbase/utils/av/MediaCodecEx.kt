@file:JvmName("MediaCodecEx")

package me.ztiany.lib.avbase.utils.av

import android.media.MediaCodecList
import timber.log.Timber

fun printMediaCodecInfo() {
    Timber.i("------------------------------------------------------------------------------------------------------")
    Timber.i("ALL_CODECS:")
    Timber.i("------------------------------------------------------------------------------------------------------")
    for (codecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
        Timber.i("${codecInfo.name}---${codecInfo.supportedTypes.contentToString()}")
    }
    Timber.i("------------------------------------------------------------------------------------------------------")
    Timber.i("REGULAR_CODECS:")
    Timber.i("------------------------------------------------------------------------------------------------------")
    for (codecInfo in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos) {
        Timber.i("${codecInfo.name}---${codecInfo.supportedTypes.contentToString()}")
    }
}