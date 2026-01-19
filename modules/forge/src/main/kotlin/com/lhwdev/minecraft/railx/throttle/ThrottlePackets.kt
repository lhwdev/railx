package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.registry.ClientboundPacketBase
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.registry.ServerboundPacketBase
import com.lhwdev.minecraft.railx.throttle.Throttles.Throttle
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer


class StartControllingPacket(
	val contraptionEntityId: Int,
	val controlsPos: BlockPos,
) : ServerboundPacketBase() {
	companion object : RailXPacketType<StartControllingPacket>() {
		override fun read(buffer: FriendlyByteBuf): StartControllingPacket = StartControllingPacket(
			contraptionEntityId = buffer.readInt(),
			controlsPos = buffer.readBlockPos(),
		)
	}
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		if(player.isSpectator) return
		
		val world = player.commandSenderWorld
		val entity = world.getEntity(contraptionEntityId) as? CarriageContraptionEntity ?: return
		
		ThrottlesServer.startControlsInput(world, entity, controlsPos, player)
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeInt(contraptionEntityId)
		buffer.writeBlockPos(controlsPos)
	}
}

class UpdateThrottlePacket(val contraptionEntityId: Int, val throttle: Throttle) : ClientboundPacketBase() {
	constructor(context: ServerThrottleControl) : this(
		contraptionEntityId = context.entity.id,
		throttle = context.throttle
	)
	
	companion object : RailXPacketType<UpdateThrottlePacket>() {
		override fun read(buffer: FriendlyByteBuf): UpdateThrottlePacket = UpdateThrottlePacket(
			contraptionEntityId = buffer.readInt(),
			throttle = ThrottlePacket.readThrottle(buffer),
		)
	}
	
	
	override fun handle(player: LocalPlayer?) {
		if(player == null) return
		if(ControlsHandler.getContraption()?.id != contraptionEntityId) return
		
		ThrottlesClient.throttle = throttle
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeInt(contraptionEntityId)
		ThrottlePacket.writeThrottle(buffer, throttle)
	}
}

class ThrottlePacket(
	val contraptionEntityId: Int,
	val controlsPos: BlockPos,
	val throttle: Throttle,
	val keys: List<Int>,
	val stopControlling: Boolean = false,
) : ServerboundPacketBase() {
	companion object : RailXPacketType<ThrottlePacket>() {
		fun readThrottle(buffer: FriendlyByteBuf): Throttle = Throttle(
			reverser = buffer.readEnum(Throttles.Reverser::class.java),
			steering = buffer.readEnum(Throttles.Steering::class.java),
			gear = buffer.readVarInt(),
		)
		
		fun writeThrottle(buffer: FriendlyByteBuf, value: Throttle) {
			buffer.writeEnum(value.reverser)
			buffer.writeEnum(value.steering)
			buffer.writeInt(value.gear)
		}
		
		override fun read(buffer: FriendlyByteBuf): ThrottlePacket = ThrottlePacket(
			contraptionEntityId = buffer.readInt(),
			controlsPos = buffer.readBlockPos(),
			throttle = readThrottle(buffer),
			keys = buffer.readList { it.readVarInt() },
			stopControlling = buffer.readBoolean(),
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
			ThrottlesServer.receiveThrottle(
				world,
				entity,
				controlsPos,
				playerId = player.uuid,
				throttle = throttle,
				keys = keys,
			)
		}
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeInt(contraptionEntityId)
		buffer.writeBlockPos(controlsPos)
		writeThrottle(buffer, throttle)
		buffer.writeCollection(keys) { buffer, key -> buffer.writeVarInt(key) }
		buffer.writeBoolean(stopControlling)
	}
}
