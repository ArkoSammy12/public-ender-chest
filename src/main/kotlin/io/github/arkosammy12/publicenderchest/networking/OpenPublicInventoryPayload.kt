package io.github.arkosammy12.publicenderchest.networking

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier
import net.minecraft.util.Uuids
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import java.util.UUID

data class OpenPublicInventoryPayload(val uuid: UUID) : CustomPayload {

    override fun getId(): Id<out CustomPayload> = PACKET_ID

    companion object {
        val PACKET_ID: Id<OpenPublicInventoryPayload> = Id(Identifier.of(PublicEnderChest.MOD_ID, "open_public_inventory"))
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, OpenPublicInventoryPayload> = Uuids.PACKET_CODEC.xmap({ uuid -> OpenPublicInventoryPayload(uuid) }, { payload -> payload.uuid }).cast()
    }

}