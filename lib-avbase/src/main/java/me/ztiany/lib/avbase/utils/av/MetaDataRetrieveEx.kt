package me.ztiany.lib.avbase.utils.av

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ztiany.lib.avbase.utils.toIntOr0

@WorkerThread
fun loadMediaMetadata(context: Context, uri: Uri): MediaMetadata {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    try {
        mediaMetadataRetriever.setDataSource(context, uri)
    } catch (e: Exception) {
        return emptyMediaMetadata()
    }
    return mediaMetadataRetriever.load()
}

suspend fun loadMediaMetadataSuspend(context: Context, uri: Uri): MediaMetadata {
    return withContext(Dispatchers.IO) {
        loadMediaMetadata(context, uri)
    }
}

@WorkerThread
fun loadMediaMetadata(path: String): MediaMetadata {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    try {
        mediaMetadataRetriever.setDataSource(path)
    } catch (e: Exception) {
        return emptyMediaMetadata()
    }
    return mediaMetadataRetriever.load()
}

suspend fun loadMediaMetadataSuspend(path: String): MediaMetadata {
    return withContext(Dispatchers.IO) {
        loadMediaMetadata(path)
    }
}

private fun emptyMediaMetadata() = MediaMetadata(0, 0, 0, 0, 0)

data class MediaMetadata(
    val rotation: Int,
    val duration: Int,
    val bitRate: Int,
    val width: Int,
    val height: Int
)

private fun MediaMetadataRetriever.load(): MediaMetadata {
    val mediaMetadata = MediaMetadata(
        rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toIntOr0(),
        duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toIntOr0(),
        bitRate = extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE).toIntOr0(),
        width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toIntOr0(),
        height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toIntOr0(),
    )
    release()
    return mediaMetadata
}
