package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket
import net.createmod.catnip.net.base.BasePacketPayload
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer


class ConfigureFlexiTrackRotatePacket(
	pos: BlockPos,
	val value: Int,
	val kind: FlexiTrackRotateScrollBehaviors.Kind,
	val mode: FlexiTrackRotateScrollBehavior.RotateStepMode,
) : BlockEntityConfigurationPacket<FlexiTrackBlockEntity>(pos) {
	companion object : RailXPacketType<ConfigureFlexiTrackRotatePacket>() {
		override val streamCodec = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ConfigureFlexiTrackRotatePacket::pos,
			ByteBufCodecs.VAR_INT, ConfigureFlexiTrackRotatePacket::value,
			ByteBufCodecs.idMapper(
				FlexiTrackRotateScrollBehaviors.Kind.entries::get,
				FlexiTrackRotateScrollBehaviors.Kind::ordinal
			),
			ConfigureFlexiTrackRotatePacket::kind,
			ByteBufCodecs.idMapper(
				FlexiTrackRotateScrollBehavior.RotateStepMode.entries::get,
				FlexiTrackRotateScrollBehavior.RotateStepMode::ordinal
			),
			ConfigureFlexiTrackRotatePacket::mode,
			::ConfigureFlexiTrackRotatePacket
		)
		
		fun save(
			pos: BlockPos,
			value: Int,
			mode: FlexiTrackRotateScrollBehavior.RotateStepMode,
		): ConfigureFlexiTrackRotatePacket = ConfigureFlexiTrackRotatePacket(
			pos,
			value,
			kind = FlexiTrackRotateScrollBehaviors.clientKind,
			mode = mode,
		)
		
		fun shortClick(
			pos: BlockPos,
			kind: FlexiTrackRotateScrollBehaviors.Kind,
		): ConfigureFlexiTrackRotatePacket = ConfigureFlexiTrackRotatePacket(
			pos,
			value = -1,
			kind,
			mode = FlexiTrackRotateScrollBehavior.RotateStepMode.Normal
		)
	}
	
	override fun applySettings(
		player: ServerPlayer,
		be: FlexiTrackBlockEntity,
	) {
		val rotateBehavior = be.allBehaviours
			.filterIsInstance<FlexiTrackRotateScrollBehaviors>()
			.firstOrNull() ?: return
		
		rotateBehavior.serverKind = kind
		if(value == -1) return
		
		rotateBehavior.setValueSettings(player, value = value, mode = mode)
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.ConfigureFlexiTrackRotate
}
