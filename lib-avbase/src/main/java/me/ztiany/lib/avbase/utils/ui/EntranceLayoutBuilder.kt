package me.ztiany.lib.avbase.utils.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import me.ztiany.lib.avbase.R

interface IEntrance {
    val title: CharSequence
}

fun buildLayoutEntrance(
    context: Context,
    list: List<IEntrance>,
    onClick: (View, Int) -> Unit
): View {
    val nestedScrollView = NestedScrollView(context)
    val linearLayout = LinearLayout(context)
    linearLayout.orientation = LinearLayout.VERTICAL
    nestedScrollView.addView(linearLayout, newMWLayoutParams())
    val layoutInflater = LayoutInflater.from(context)
    for (i in list.indices) {
        layoutInflater.inflate(R.layout.common_btn, linearLayout, true)
        (linearLayout.getChildAt(linearLayout.childCount - 1) as TextView).apply {
            text = list[i].title
            setOnClickListener {
                onClick(it, i)
            }
        }
    }

    return nestedScrollView
}