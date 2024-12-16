@file:JvmName("Resources")

package me.ztiany.lib.avbase.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import com.blankj.utilcode.util.Utils

fun loadBitmap(@DrawableRes resourceId: Int): Bitmap =
    BitmapFactory.decodeResource(Utils.getApp().resources, resourceId)
