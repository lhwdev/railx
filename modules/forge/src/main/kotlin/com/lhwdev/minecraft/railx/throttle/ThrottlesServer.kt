package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler
import net.createmod.catnip.data.IntAttached
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import java.lang.invoke.MethodHandles
import java.util.*


object ThrottlesServer {
	val receivedThrottles: WorldAttached<MutableMap<UUID, ServerThrottle>> =
		WorldAttached { mutableMapOf() }
	
	
	fun tick(world: LevelAccessor) {
		if(RailXConfig.Server.throttle.enabled.isFalse) return
		
		val worldThrottles = receivedThrottles[world].iterator()
		val worldControls = ControlsServerHandler.receivedInputs[world]
		while(worldThrottles.hasNext()) {
			val (playerId, context) = worldThrottles.next()
			
			if(context.entity.isRemoved) {
				worldThrottles.remove()
				worldControls.remove(playerId)
				continue
			}
			
			val keys = ContextAccessor.getKeys(worldControls[playerId]!!)
			val keyIterator = keys.iterator()
			while(keyIterator.hasNext()) {
				val key = keyIterator.next()
				key.decrement()
				if(key.first <= 0) keyIterator.remove()
			}
			
			val player = world.getPlayerByUUID(playerId)
			if(player == null) {
				context.entity.stopControlling(context.controlsPos)
				worldThrottles.remove()
				worldControls.remove(playerId)
				continue
			}
			
			context.entity.control(
				context.controlsPos,
				ThrottleStubs.HeldControls(context.throttle, keys.map { it.second }),
				player,
			)
			
			if(keys.isEmpty()) {
				worldThrottles.remove()
				worldControls.remove(playerId)
			}
		}
	}
	
	@Suppress("NOTHING_TO_INLINE")
	private object ContextAccessor {
		val context = ControlsServerHandler::class.java.declaredClasses
			.first { it.name.endsWith("ControlsContext") }
		
		val keys = context.getDeclaredField("keys")
			.also { it.isAccessible = true }
			.let { MethodHandles.lookup().unreflectGetter(it) }
		
		@Suppress("UNCHECKED_CAST")
		inline fun getKeys(context: Any) =
			keys.invoke(context) as MutableCollection<IntAttached<Int>>
	}
	
	fun receiveThrottle(
		world: LevelAccessor,
		entity: AbstractContraptionEntity,
		controlsPos: BlockPos,
		uniqueId: UUID,
		throttle: Throttles.Throttle,
	) {
		val worldThrottles = receivedThrottles[world]
		if(worldThrottles[uniqueId]?.let { it.entity != entity } == true)
			worldThrottles -= uniqueId
		
		val context = worldThrottles.getOrPut(uniqueId) { ServerThrottle(entity, controlsPos) }
		context.throttle = throttle
	}
}


class ServerThrottle(
	val entity: AbstractContraptionEntity,
	var controlsPos: BlockPos,
) {
	var throttle: Throttles.Throttle = Throttles.Throttle.Neutral
}
