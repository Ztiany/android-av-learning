package me.ztiany.lib.avbase.utils.av

import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.ztiany.lib.avbase.utils.appScope
import timber.log.Timber
import java.io.IOException

fun printMediaTrackInfo(context: Context, uri: Uri) {
    appScope.launch(Dispatchers.IO) {
        MediaExtractor().apply {
            try {
                setDataSource(context, uri, null)
                for (i in 0 until trackCount) {
                    Timber.d("tract $i-->${getTrackFormat(i)}")
                }
            } catch (e: IOException) {
                Timber.e(e, "printMediaTrackInfo")
            }
        }
    }
}