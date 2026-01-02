package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.registry.ServerboundPacketBase
import com.lhwdev.minecraft.railx.throttle.Throttles.Throttle
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer


class ThrottlePacket(
	val contraptionEntityId: Int,
	val controlsPos: BlockPos,
	val throttle: Throttle,
	val otherKeys: List<Int>,
	val stopControlling: Boolean = false,
) : ServerboundPacketBase() {
	companion object : RailXPacketType<ThrottlePacket>() {
		override fun read(buffer: FriendlyByteBuf): ThrottlePacket = ThrottlePacket(
			contraptionEntityId = buffer.readInt(),
			controlsPos = buffer.readBlockPos(),
			throttle = Throttle(
				reverser = buffer.readEnum(Throttles.Reverser::class.java),
				steering = buffer.readEnum(Throttles.Steering::class.java),
				gear = buffer.readVarInt(),
			),
			otherKeys = buffer.readList { it.readVarInt() },
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
				otherKeys = otherKeys,
			)
		}
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeInt(contraptionEntityId)
		buffer.writeBlockPos(controlsPos)
		
		buffer.writeEnum(throttle.reverser)
		buffer.writeEnum(throttle.steering)
		buffer.writeVarInt(throttle.gear)
		
		buffer.writeCollection(otherKeys) { buffer, value -> buffer.writeVarInt(value) }
		buffer.writeBoolean(stopControlling)
	}
}
