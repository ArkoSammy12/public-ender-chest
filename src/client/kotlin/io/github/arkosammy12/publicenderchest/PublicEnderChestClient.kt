package io.github.arkosammy12.publicenderchest

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import io.github.arkosammy12.publicenderchest.networking.C2SHandshakeResponsePayload
import io.github.arkosammy12.publicenderchest.networking.OpenPublicInventoryPayload
import io.github.arkosammy12.publicenderchest.networking.S2CHandshakeRequestPayload
import java.util.*

object PublicEnderChestClient : ClientModInitializer {

	override fun onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(S2CHandshakeRequestPayload.PACKET_ID) { payload, context ->
			context.responseSender().sendPacket(C2SHandshakeResponsePayload(UUID.randomUUID()))
		}
	}

	@JvmStatic
	fun sendOpenPublicInventoryPayload() {
		ClientPlayNetworking.send(OpenPublicInventoryPayload(UUID.randomUUID()))
	}

}