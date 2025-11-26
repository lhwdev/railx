package com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver

import com.lhwdev.minecraft.railx.registry.BlockEntityConfigurationPacket
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.registry.ServerboundPacket
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer


class ObserverEditPacket(pos: BlockPos, val code: String) :
	BlockEntityConfigurationPacket<AdvancedTrackObserverBlockEntity>(pos), ServerboundPacket {
	companion object : RailXPacketType<ObserverEditPacket>() {
		override fun read(buffer: FriendlyByteBuf): ObserverEditPacket = ObserverEditPacket(
			pos = buffer.readBlockPos(),
			code = buffer.readUtf(512),
		)
	}
	
	init {
		check(code.length <= 512) { "code too long" }
	}
	
	override fun applySettings(
		player: ServerPlayer,
		be: AdvancedTrackObserverBlockEntity,
	) {
		be.rule.code = code
		be.onRuleUpdated()
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeBlockPos(pos)
		buffer.writeUtf(code)
	}
}
