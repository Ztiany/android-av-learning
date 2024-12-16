package me.ztiany.androidav.opengl.nwopengl

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.lib.avbase.utils.ui.IEntrance
import me.ztiany.lib.avbase.utils.ui.buildLayoutEntrance

private const val RENDER_TYPE_BACKGROUND = 0

class NativeWithOpenGLMainActivity : AppCompatActivity() {

    private data class CommonItem(
        override val title: String,
        val rendererType: Int
    ) : IEntrance

    private val entrances = listOf(
        CommonItem("绘制背景", RENDER_TYPE_BACKGROUND),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            buildLayoutEntrance(
                this,
                entrances
            ) { _, index ->
                handleClicked(index)
            }
        )
    }

    private fun handleClicked(index: Int) {
        val item = entrances[index]
        if (item is CommonItem) {
            NativeWithOpenGLCommonActivity.start(this, item.title, item.rendererType)
        }
    }

}