package me.ztiany.androidav.player.mediacodec

import java.nio.ByteBuffer

interface MediaDataProvider {

    fun readPacket(buffer: ByteBuffer, packet: PacketInfo? = null): Int

}