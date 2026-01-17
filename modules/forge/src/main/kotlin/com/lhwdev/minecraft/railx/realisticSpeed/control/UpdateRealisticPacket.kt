package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ClientboundPacketPayload
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.codec.StreamCodec


class UpdateRealisticPacket(val parameters: RealisticSpeedParameters) : ClientboundPacketPayload {
	companion object : RailXPacketType<UpdateRealisticPacket>() {
		override val streamCodec = StreamCodec.composite(
			RealisticSpeedParameters.streamCodec, UpdateRealisticPacket::parameters,
			::UpdateRealisticPacket,
		)
	}
	
	override fun handle(player: LocalPlayer?) {
		RealisticSpeedClient.parameters = parameters
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.UpdateRealistic
}
