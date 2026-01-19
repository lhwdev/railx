package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.LevelAccessor
import java.util.*


object ThrottlesServer {
	class LevelThrottles {
		val byPlayer = mutableMapOf<UUID, ServerThrottleControl>()
		val byTrain = mutableMapOf<UUID, ServerThrottleControl>()
		
		val values: Collection<ServerThrottleControl>
			get() = byPlayer.values
		
		fun remove(context: ServerThrottleControl) {
			byPlayer.remove(context.playerId)
			byTrain.remove(context.trainId)
		}
		
		inline fun getOrPut(playerId: UUID, create: () -> ServerThrottleControl): ServerThrottleControl {
			byPlayer[playerId]?.let { return it }
			val context = create()
			byPlayer[context.playerId] = context
			byTrain[context.trainId] = context
			return context
		}
		
		fun clear() {
			byPlayer.clear()
			byTrain.clear()
		}
	}
	
	val receivedThrottles: WorldAttached<LevelThrottles> =
		WorldAttached { LevelThrottles() }
	
	
	fun tick(world: LevelAccessor) {
		if(!RailXConfig.Server.throttle.enabled.get()) {
			receivedThrottles[world].clear()
			return
		}
		
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
			
			val keys = mutableSetOf<Int>()
			val keysIterator = context.keys.iterator()
			while(keysIterator.hasNext()) {
				val key = keysIterator.next()
				key.life--
				if(!key.isAlive) keysIterator.remove()
				else keys += key.key
			}
			
			val throttle = context.throttle
			val absoluteThrottle = throttle.reverseIf(forward = context.forward)
			if(throttle.gear >= 0) when(throttle.reverser) {
				Throttles.Reverser.Forward -> keys += 0
				Throttles.Reverser.Neutral -> {}
				Throttles.Reverser.Backward -> keys += 1
			}
			when(throttle.steering) {
				Throttles.Steering.Left -> keys += 2
				Throttles.Steering.Neutral -> {}
				Throttles.Steering.Right -> keys += 3
			}
			
			context.entity.carriage.train.absoluteThrottle = absoluteThrottle
			
			val controlled = context.entity.control(context.controlsPos, keys, player)
			if(!controlled) context.entity.stopControlling(context.controlsPos)
		}
	}
	
	private fun getOrCreateContext(
		world: LevelAccessor,
		playerId: UUID,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
	): ServerThrottleControl {
		val worldThrottles = receivedThrottles[world]
		worldThrottles.byPlayer[playerId]?.let {
			if(it.entity != entity) worldThrottles.remove(it)
		}
		
		worldThrottles.byTrain[entity.carriage.train.id]?.let {
			if(it.playerId != playerId) worldThrottles.remove(it)
		}
		
		val context =
			worldThrottles.getOrPut(playerId = playerId) { ServerThrottleControl(playerId, entity, controlsPos) }
		context.updateControlsPos(controlsPos)
		return context
	}
	
	fun receiveThrottle(
		world: LevelAccessor,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
		playerId: UUID,
		throttle: Throttles.Throttle,
		keys: Collection<Int>,
	) {
		val context = getOrCreateContext(world, playerId, entity, controlsPos)
		context.updateControlsPos(controlsPos)
		context.throttle = throttle
		
		for(key in context.keys) {
			if(key.key in keys) key.keepAlive()
			else key.life = 0
		}
		for(key in keys) {
			if(context.keys.any { it.key == key }) continue
			context.keys += ServerThrottleControl.ManuallyPressedKey(key)
		}
	}
	
	fun startControlsInput(
		world: LevelAccessor,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
		player: ServerPlayer,
	) {
		val context = getOrCreateContext(world, playerId = player.uuid, entity, controlsPos)
		AllPackets.sendToPlayer(player, UpdateThrottlePacket(context))
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
		if(pressed) for(key in keys) {
			if(context.keys.any { it.key == key }) continue
			context.keys += ServerThrottleControl.ManuallyPressedKey(key)
		} else for(key in keys) {
			val previous = context.keys.find { it.key == key } ?: continue
			previous.life = 0
		}
	}
}


class ServerThrottleControl(
	val playerId: UUID,
	val entity: CarriageContraptionEntity,
	controlsPos: BlockPos,
) {
	var controlsPos: BlockPos = controlsPos
		private set
	
	val forward: Boolean
		get() {
			val info = entity.contraption.blocks[controlsPos] ?: return true
			if(!info.state.hasProperty(ControlsBlock.FACING)) return true
			
			return info.state.getValue(ControlsBlock.FACING) == entity.initialOrientation.counterClockWise
		}
	
	var throttle: Throttles.Throttle = entity.carriage.train.relativeThrottle(forward = forward)
	val keys = mutableSetOf<ManuallyPressedKey>()
	
	fun updateControlsPos(pos: BlockPos) {
		if(controlsPos == pos) return
		
		val previousForward = forward
		controlsPos = pos
		if(forward != previousForward) throttle = throttle.reverse()
	}
	
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
