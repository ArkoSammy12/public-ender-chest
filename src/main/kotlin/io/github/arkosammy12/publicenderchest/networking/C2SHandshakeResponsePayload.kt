package io.github.arkosammy12.publicenderchest.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier
import net.minecraft.util.Uuids
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import java.util.UUID

data class C2SHandshakeResponsePayload(val uuid: UUID) : CustomPayload {

    override fun getId(): Id<out CustomPayload> = PACKET_ID

    companion object {
        val PACKET_ID: Id<C2SHandshakeResponsePayload> = Id(Identifier.of(PublicEnderChest.MOD_ID, "c2s_handshake_response"))
        val PACKET_CODEC: PacketCodec<PacketByteBuf, C2SHandshakeResponsePayload> = Uuids.PACKET_CODEC.xmap({ uuid -> C2SHandshakeResponsePayload(uuid) }, { payload -> payload.uuid }).cast()
    }

}