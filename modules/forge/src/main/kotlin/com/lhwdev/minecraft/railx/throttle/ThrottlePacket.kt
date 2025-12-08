package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.throttle.Throttles.Throttle
import com.lhwdev.minecraft.railx.utils.StreamCodecs
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ServerboundPacketPayload
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer


class ThrottlePacket(
	val contraptionEntityId: Int,
	val controlsPos: BlockPos,
	val throttle: Throttle,
	val otherKeys: List<Int>,
	val stopControlling: Boolean = false,
) : ServerboundPacketPayload {
	companion object : RailXPacketType<ThrottlePacket>() {
		private val throttleCodec = StreamCodec.composite(
			StreamCodecs.enum(), Throttle::reverser,
			StreamCodecs.enum(), Throttle::steering,
			ByteBufCodecs.VAR_INT, Throttle::gear,
			::Throttle,
		)
		
		override val streamCodec = StreamCodec.composite(
			ByteBufCodecs.INT, ThrottlePacket::contraptionEntityId,
			BlockPos.STREAM_CODEC, ThrottlePacket::controlsPos,
			throttleCodec, ThrottlePacket::throttle,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ThrottlePacket::otherKeys,
			ByteBufCodecs.BOOL, ThrottlePacket::stopControlling,
			::ThrottlePacket
		)
	}
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		if(player.isSpectator) return
		
		val world = player.commandSenderWorld
		val entity = world.getEntity(contraptionEntityId) as? AbstractContraptionEntity ?: return
		
		if(stopControlling) {
			entity.stopControlling(controlsPos)
			return
		}
		
		if(entity.toGlobalVector(controlsPos.center, 0f).closerThan(player.position(), 16.0)) {
			ThrottlesServer.receiveThrottle(
				world,
				entity,
				controlsPos,
				uniqueId = player.uuid,
				throttle = throttle,
				otherKeys = otherKeys,
			)
		}
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.Throttle
}
