package me.ztiany.androidav.avapi.video.combine

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ztiany.androidav.R
import me.ztiany.lib.avbase.utils.Directory
import java.io.File

/**
 * 简易的视频拼接，如果两个视频的音视频规格不一致，则合并后的视频是有问题的。
 * 具体参考 [merging-concat-videos-in-android-through-ffmpeg](https://stackoverflow.com/questions/46724197/merging-concat-videos-in-android-through-ffmpeg)。
 * 要实现任意格式的视频拼接，则需要使用 FFmpeg 来首先，先对音视频进行重采样，统一格式后进行拼接。
 */
class VideoCombinationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_mosaic_video)
    }

    fun execute(view: android.view.View) {
        val originVideo1 = Directory.createSDCardRootAppPath("input1.mp4")
        val originVideo2 = Directory.createSDCardRootAppPath("input2.mp4")

        if (originVideo1.exists() && originVideo2.exists()) {
            lifecycleScope.launch {
                view.isEnabled = false
                val result = withContext(Dispatchers.IO) {
                    doMosaic(originVideo1, originVideo2)
                }
                if (result) {
                    Toast.makeText(this@VideoCombinationActivity, "拼接成功", Toast.LENGTH_LONG).show()
                }
                view.isEnabled = true
            }
        }
    }

    private fun doMosaic(originVideo1: File, originVideo2: File) = try {
        VideoCombining.combine(
            originVideo1.absolutePath,
            originVideo2.absolutePath,
            Directory.createSDCardRootAppPath("mosaic_result.mp4").absolutePath
        )
    } catch (e: Exception) {
        false
    }

}