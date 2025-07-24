package com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacket
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket
import net.createmod.catnip.net.base.BasePacketPayload
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer


class ObserverEditPacket(pos: BlockPos, val code: String) :
	BlockEntityConfigurationPacket<AdvancedTrackObserverBlockEntity>(pos) {
	companion object : RailXPacket<ObserverEditPacket>() {
		override val streamCodec = StreamCodec.composite(
			BlockPos.STREAM_CODEC, { it.pos },
			ByteBufCodecs.STRING_UTF8, { it.code },
			::ObserverEditPacket
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
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.ObserverEdit
}
