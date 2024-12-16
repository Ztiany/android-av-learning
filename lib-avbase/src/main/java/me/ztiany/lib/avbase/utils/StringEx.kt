package me.ztiany.lib.avbase.utils

fun String?.toIntOr0(): Int {
    return if (isNullOrEmpty()) 0 else toInt()
}