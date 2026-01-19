package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.lhwdev.minecraft.railx.registry.ClientboundPacketBase
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.FriendlyByteBuf


class UpdateRealisticPacket(val parameters: RealisticSpeedParameters) : ClientboundPacketBase() {
	companion object : RailXPacketType<UpdateRealisticPacket>() {
		override fun read(buffer: FriendlyByteBuf): UpdateRealisticPacket =
			UpdateRealisticPacket(parameters = RealisticSpeedParameters.read(buffer))
	}
	
	override fun handle(player: LocalPlayer?) {
		RealisticSpeedClient.parameters = parameters
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		parameters.write(buffer)
	}
}
