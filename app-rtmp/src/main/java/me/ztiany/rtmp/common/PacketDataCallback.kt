package me.ztiany.rtmp.common

interface PacketDataCallback {

    fun onPacket(packet: Packet)

}