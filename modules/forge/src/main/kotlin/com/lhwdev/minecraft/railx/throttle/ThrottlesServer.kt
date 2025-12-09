package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import java.util.*


object ThrottlesServer {
	val receivedThrottles: WorldAttached<MutableMap<UUID, ServerThrottle>> =
		WorldAttached { mutableMapOf() }
	
	
	fun tick(world: LevelAccessor) {
		if(!RailXConfig.Server.throttle.enabled.get()) return
		
		val worldThrottles = receivedThrottles[world].iterator()
		while(worldThrottles.hasNext()) {
			val (playerId, context) = worldThrottles.next()
			
			if(context.entity.isRemoved) {
				worldThrottles.remove()
				continue
			}
			
			val player = world.getPlayerByUUID(playerId)
			if(player == null) {
				context.entity.stopControlling(context.controlsPos)
				worldThrottles.remove()
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
	
	fun receiveThrottle(
		world: LevelAccessor,
		entity: AbstractContraptionEntity,
		controlsPos: BlockPos,
		uniqueId: UUID,
		throttle: Throttles.Throttle,
		otherKeys: Collection<Int>,
	) {
		val worldThrottles = receivedThrottles[world]
		if(worldThrottles[uniqueId]?.let { it.entity != entity } == true)
			worldThrottles -= uniqueId
		
		val context = worldThrottles.getOrPut(uniqueId) { ServerThrottle(entity, controlsPos) }
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
		entity: AbstractContraptionEntity,
		controlsPos: BlockPos,
		uniqueId: UUID,
		keys: Collection<Int>,
		pressed: Boolean,
	) {
		val worldThrottles = receivedThrottles[world]
		if(worldThrottles[uniqueId]?.let { it.entity != entity } == true)
			worldThrottles -= uniqueId
		
		val context = worldThrottles.getOrPut(uniqueId) { ServerThrottle(entity, controlsPos) }
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
	val entity: AbstractContraptionEntity,
	var controlsPos: BlockPos,
) {
	var throttle: Throttles.Throttle = Throttles.Throttle.Neutral
	val keys = mutableSetOf<ManuallyPressedKey>()
	
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
