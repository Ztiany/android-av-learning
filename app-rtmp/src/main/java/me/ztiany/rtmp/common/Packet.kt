package me.ztiany.rtmp.common

class Packet(
    val byteArray: ByteArray,
    val presentationTime: Long,
    val type: Int
)

const val TYPE_AUDIO_INFO = 1
const val TYPE_AUDIO_DATA = 2
const val TYPE_VIDEO_DATA = 3