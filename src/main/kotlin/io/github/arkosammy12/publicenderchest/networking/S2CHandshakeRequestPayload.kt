package io.github.arkosammy12.publicenderchest.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier
import net.minecraft.util.Uuids
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import java.util.UUID

data class S2CHandshakeRequestPayload(val uuid: UUID) : CustomPayload {

    override fun getId(): Id<out CustomPayload> = PACKET_ID

    companion object {
        val PACKET_ID: Id<S2CHandshakeRequestPayload> = Id(Identifier.of(PublicEnderChest.MOD_ID, "s2c_handshake_request"))
        val PACKET_CODEC: PacketCodec<PacketByteBuf, S2CHandshakeRequestPayload> = Uuids.PACKET_CODEC.xmap({ uuid -> S2CHandshakeRequestPayload(uuid) }, { payload -> payload.uuid }).cast()
    }

}