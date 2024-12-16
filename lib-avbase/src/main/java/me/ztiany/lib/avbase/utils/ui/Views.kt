@file:JvmName("Views")

package me.ztiany.lib.avbase.utils.ui

import android.view.ViewGroup

fun newMMLayoutParams() =
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

fun newMWLayoutParams() =
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
