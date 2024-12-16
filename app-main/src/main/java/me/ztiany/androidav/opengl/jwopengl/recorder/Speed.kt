package me.ztiany.androidav.opengl.jwopengl.recorder

enum class Speed {
    MODE_EXTRA_SLOW, MODE_SLOW, MODE_NORMAL, MODE_FAST, MODE_EXTRA_FAST
}

fun Speed.getSpeedValue(): Float {
    return when (this) {
        Speed.MODE_EXTRA_SLOW -> 0.3F
        Speed.MODE_SLOW -> 0.5F
        Speed.MODE_FAST -> 2F
        Speed.MODE_EXTRA_FAST -> 3F
        Speed.MODE_NORMAL -> 1F
    }
}

fun Speed.getSpeedName(): String {
    return when (this) {
        Speed.MODE_EXTRA_SLOW -> "%s 倍数".format("0.3")
        Speed.MODE_SLOW -> "%s 倍数".format("0.5")
        Speed.MODE_FAST -> "%s 倍数".format("2.0")
        Speed.MODE_EXTRA_FAST -> "%s 倍数".format("3.0")
        Speed.MODE_NORMAL -> "%s 倍数".format("1.0")
    }
}