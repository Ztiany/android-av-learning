package me.ztiany.androidav.avtest

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.ztiany.lib.avbase.utils.av.loadMediaMetadataSuspend
import timber.log.Timber

class MediaMetadataRetrieveCase(
    private val lifecycleOwner: AppCompatActivity,
    private val uri: Uri
) : TestCase {

    override fun start() {
        lifecycleOwner.lifecycleScope.launch {
            Timber.d("uri: %s", loadMediaMetadataSuspend(lifecycleOwner, uri).toString())
        }
    }

    override fun stop() {

    }

}