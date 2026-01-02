package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import java.util.*


object ThrottlesServer {
	class Throttles {
		val byPlayer = mutableMapOf<UUID, ServerThrottle>()
		val byTrain = mutableMapOf<UUID, ServerThrottle>()
		
		fun remove(context: ServerThrottle) {
			byPlayer.remove(context.playerId)
			byTrain.remove(context.trainId)
		}
		
		inline fun getOrPut(playerId: UUID, create: () -> ServerThrottle): ServerThrottle {
			byPlayer[playerId]?.let { return it }
			val context = create()
			byPlayer[context.playerId] = context
			byTrain[context.trainId] = context
			return context
		}
	}
	
	val receivedThrottles: WorldAttached<Throttles> =
		WorldAttached { Throttles() }
	
	
	fun tick(world: LevelAccessor) {
		if(!RailXConfig.Server.throttle.enabled.get()) return
		
		val worldThrottles = receivedThrottles[world]
		val byPlayer = worldThrottles.byPlayer.iterator()
		while(byPlayer.hasNext()) {
			val (playerId, context) = byPlayer.next()
			
			if(context.entity.isRemoved) {
				worldThrottles.remove(context)
				continue
			}
			
			val player = world.getPlayerByUUID(playerId)
			if(player == null) {
				context.entity.stopControlling(context.controlsPos)
				worldThrottles.remove(context)
				continue
			}
			
			val keys = mutableListOf<Int>()
			val keysIterator = context.keys.iterator()
			while(keysIterator.hasNext()) {
				val key = keysIterator.next()
				key.life--
				if(!key.isAlive) keysIterator.remove()
				else keys += key.key
			}
			
			val controlled = context.entity.control(
				context.controlsPos,
				ThrottleStubs.HeldControls(context.throttle, keys),
				player,
			)
			if(!controlled) context.entity.stopControlling(context.controlsPos)
		}
	}
	
	private fun getOrCreateContext(
		world: LevelAccessor,
		playerId: UUID,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
	): ServerThrottle {
		val worldThrottles = receivedThrottles[world]
		worldThrottles.byPlayer[playerId]?.let {
			if(it.entity != entity) worldThrottles.remove(it)
		}
		
		worldThrottles.byTrain[entity.carriage.train.id]?.let {
			if(it.playerId != playerId) worldThrottles.remove(it)
		}
		
		return worldThrottles.getOrPut(playerId = playerId) { ServerThrottle(playerId, entity, controlsPos) }
	}
	
	fun receiveThrottle(
		world: LevelAccessor,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
		playerId: UUID,
		throttle: Throttles.Throttle,
		otherKeys: Collection<Int>,
	) {
		val context = getOrCreateContext(world, playerId, entity, controlsPos)
		context.controlsPos = controlsPos
		context.throttle = throttle
		for(key in context.keys) {
			if(key.key in otherKeys) key.keepAlive()
			else key.life = 0
		}
		for(key in otherKeys) {
			if(context.keys.any { it.key == key }) continue
			context.keys += ServerThrottle.ManuallyPressedKey(key)
		}
	}
	
	fun receiveControlsInput(
		world: LevelAccessor,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
		playerId: UUID,
		keys: Collection<Int>,
		pressed: Boolean,
	) {
		val context = getOrCreateContext(world, playerId, entity, controlsPos)
		context.controlsPos = controlsPos
		if(pressed) for(key in keys) {
			if(context.keys.any { it.key == key }) continue
			context.keys += ServerThrottle.ManuallyPressedKey(key)
		} else for(key in keys) {
			val previous = context.keys.find { it.key == key } ?: continue
			previous.life = 0
		}
	}
}


class ServerThrottle(
	val playerId: UUID,
	val entity: CarriageContraptionEntity,
	var controlsPos: BlockPos,
) {
	var throttle: Throttles.Throttle = Throttles.Throttle.Neutral
	val keys = mutableSetOf<ManuallyPressedKey>()
	
	val trainId: UUID
		get() = entity.carriage.train.id
	
	class ManuallyPressedKey(val key: Int) {
		var life: Int = Timeout
		
		val isAlive: Boolean get() = life > 0
		
		fun keepAlive() {
			life = Timeout
		}
		
		companion object {
			const val Timeout: Int = 30
		}
	}
}
