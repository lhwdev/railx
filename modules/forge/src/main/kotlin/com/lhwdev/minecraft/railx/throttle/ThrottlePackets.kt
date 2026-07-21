package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.throttle.Throttles.Throttle
import com.lhwdev.minecraft.railx.utils.StreamCodecs
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ClientboundPacketPayload
import net.createmod.catnip.net.base.ServerboundPacketPayload
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer


class StartControllingPacket(
	val contraptionEntityId: Int,
	val controlsPos: BlockPos,
) : ServerboundPacketPayload {
	companion object : RailXPacketType<StartControllingPacket>() {
		override val streamCodec = StreamCodec.composite(
			ByteBufCodecs.INT, StartControllingPacket::contraptionEntityId,
			BlockPos.STREAM_CODEC, StartControllingPacket::controlsPos,
			::StartControllingPacket,
		)
	}
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		if(player.isSpectator) return
		
		val world = player.commandSenderWorld
		val entity = world.getEntity(contraptionEntityId) as? CarriageContraptionEntity ?: return
		
		ThrottlesServer.startControlsInput(world, entity, controlsPos, player)
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.StartControlling
}

class UpdateThrottlePacket(val contraptionEntityId: Int, val throttle: Throttle) : ClientboundPacketPayload {
	constructor(context: ServerThrottleControl) : this(
		contraptionEntityId = context.entity.id,
		throttle = context.relativeThrottle
	)
	
	companion object : RailXPacketType<UpdateThrottlePacket>() {
		override val streamCodec = StreamCodec.composite(
			ByteBufCodecs.INT, UpdateThrottlePacket::contraptionEntityId,
			ThrottlePacket.throttleCodec, UpdateThrottlePacket::throttle,
			::UpdateThrottlePacket,
		)
	}
	
	
	override fun handle(player: LocalPlayer?) {
		if(player == null) return
		if(ControlsHandler.getContraption()?.id != contraptionEntityId) return
		
		ThrottlesClient.throttle = throttle
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.UpdateThrottle
}

class ThrottlePacket(
	val contraptionEntityId: Int,
	val controlsPos: BlockPos,
	val throttle: Throttle,
	val keys: List<Int>,
	val stopControlling: Boolean = false,
) : ServerboundPacketPayload {
	companion object : RailXPacketType<ThrottlePacket>() {
		val throttleCodec = StreamCodec.composite(
			StreamCodecs.enum(), Throttle::reverser,
			StreamCodecs.enum(), Throttle::steering,
			ByteBufCodecs.VAR_INT, Throttle::gear,
			::Throttle,
		)
		
		override val streamCodec = StreamCodec.composite(
			ByteBufCodecs.INT, ThrottlePacket::contraptionEntityId,
			BlockPos.STREAM_CODEC, ThrottlePacket::controlsPos,
			throttleCodec, ThrottlePacket::throttle,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ThrottlePacket::keys,
			ByteBufCodecs.BOOL, ThrottlePacket::stopControlling,
			::ThrottlePacket
		)
	}
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		if(player.isSpectator) return
		
		val world = player.commandSenderWorld
		val entity = world.getEntity(contraptionEntityId) as? CarriageContraptionEntity ?: return
		
		if(stopControlling) {
			entity.stopControlling(controlsPos)
			return
		}
		
		if(entity.toGlobalVector(controlsPos.center, 0f).closerThan(player.position(), 16.0)) {
			ThrottlesServer.receiveControl(
				world,
				entity,
				controlsPos,
				playerId = player.uuid,
				relativeThrottle = throttle,
				keys = keys,
			)
		}
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.Throttle
}
