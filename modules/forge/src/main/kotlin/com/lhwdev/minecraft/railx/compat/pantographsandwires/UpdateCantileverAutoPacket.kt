package com.lhwdev.minecraft.railx.compat.pantographsandwires

import com.lhwdev.minecraft.railx.registry.BlockEntityConfigurationPacket
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.registry.ServerboundPacket
import de.mrjulsen.paw.blockentity.CantileverBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

class UpdateCantileverAutoPacket(pos: BlockPos, val calculated: AutoCantilever.Calculated) :
	BlockEntityConfigurationPacket<CantileverBlockEntity>(pos), ServerboundPacket {
	companion object : RailXPacketType<UpdateCantileverAutoPacket>() {
		override fun read(buffer: FriendlyByteBuf): UpdateCantileverAutoPacket = UpdateCantileverAutoPacket(
			pos = buffer.readBlockPos(),
			calculated = AutoCantilever.Calculated.read(buffer),
		)
	}
	
	override fun applySettings(player: ServerPlayer, be: CantileverBlockEntity) {
		calculated.applyTo(be)
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeBlockPos(pos)
		calculated.write(buffer)
	}
}
