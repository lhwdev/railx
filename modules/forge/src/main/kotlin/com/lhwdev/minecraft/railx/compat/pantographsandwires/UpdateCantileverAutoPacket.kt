package com.lhwdev.minecraft.railx.compat.pantographsandwires

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket
import de.mrjulsen.paw.blockentity.CantileverBlockEntity
import net.createmod.catnip.net.base.BasePacketPayload
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer

class UpdateCantileverAutoPacket(pos: BlockPos, val calculated: AutoCantilever.Calculated) :
	BlockEntityConfigurationPacket<CantileverBlockEntity>(pos) {
	companion object : RailXPacketType<UpdateCantileverAutoPacket>() {
		override val streamCodec = StreamCodec.composite(
			BlockPos.STREAM_CODEC, UpdateCantileverAutoPacket::pos,
			AutoCantilever.Calculated.streamCodec, UpdateCantileverAutoPacket::calculated,
			::UpdateCantileverAutoPacket,
		)
	}
	
	override fun applySettings(player: ServerPlayer, be: CantileverBlockEntity) {
		calculated.applyTo(be)
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.UpdateCantileverAuto
}
