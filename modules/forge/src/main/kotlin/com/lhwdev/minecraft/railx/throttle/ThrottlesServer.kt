package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.realisticSpeed.realisticSpeed
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.infrastructure.config.AllConfigs
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.LevelAccessor
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*
import kotlin.math.abs
import kotlin.math.min


object ThrottlesServer {
	class LevelControls {
		val byPlayer = mutableMapOf<UUID, ServerThrottleControl>()
		
		fun removeControl(context: ServerThrottleControl) {
			byPlayer.remove(context.playerId)
		}
		
		inline fun getOrPutControl(playerId: UUID, create: () -> ServerThrottleControl): ServerThrottleControl {
			byPlayer[playerId]?.let { return it }
			val context = create()
			byPlayer[context.playerId] = context
			return context
		}
		
		fun clear() {
			byPlayer.clear()
		}
	}
	
	val receivedControls: WorldAttached<LevelControls> =
		WorldAttached { LevelControls() }
	
	fun tick(world: LevelAccessor) {
		if(RailXConfig.Server.throttle.enabled.isFalse) {
			receivedControls[world].clear()
			return
		}
		
		val worldThrottles = receivedControls[world]
		val playerThrottlesToRemove = mutableListOf<ServerThrottleControl>()
		
		for((playerId, context) in worldThrottles.byPlayer) {
			val player = world.getPlayerByUUID(playerId)
			if(player == null) {
				context.entity.stopControlling(context.controlsPos)
				playerThrottlesToRemove += context
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
			
			val actualKeys = mutableSetOf<Int>()
			val throttle = context.throttle
			val relativeThrottle = context.relativeThrottle
			if(relativeThrottle.gear >= 0) when(relativeThrottle.reverser) {
				Throttles.Reverser.Forward -> actualKeys += 0
				Throttles.Reverser.Neutral -> {}
				Throttles.Reverser.Backward -> actualKeys += 1
			}
			when(relativeThrottle.steering) {
				Throttles.Steering.Left -> actualKeys += 2
				Throttles.Steering.Neutral -> {}
				Throttles.Steering.Right -> actualKeys += 3
			}
			
			for(key in keys) {
				if(key > 3) actualKeys += key
			}
			
			context.entity.carriage.train.absoluteThrottle = throttle
			
			val controlled = context.entity.control(context.controlsPos, actualKeys, player)
			if(!controlled) context.entity.stopControlling(context.controlsPos)
		}
		for(toRemove in playerThrottlesToRemove)
			worldThrottles.removeControl(toRemove)
	}
	
	fun tickTrain(train: Train) {
		if(train.navigation.destination != null) return
		
		val throttle = train.absoluteThrottle
		if(RailXConfig.Server.realisticSpeed.enabled.isTrue) {
			val realisticSpeed = train.realisticSpeed
			if(realisticSpeed != null) {
				realisticSpeed.handleManualThrottle(throttle, train.targetSpeed)
				return
			}
		}
		
		// NOTE: should be identical with CarriageContraptionEntity.control logic
		// - Patched `slow` to be always false, as we cannot determine controlPos here
		var topSpeed = (train.maxSpeed() * AllConfigs.server().trains.manualTrainSpeedModifier.f).toDouble()
		val cappedTopSpeed = topSpeed * train.throttle
		
		// NOTE: this logic differs from CarriageContraptionEntity.control which only sees current carraige
		if(
			train.carriages.first().leadingPoint.edge?.isTurn == true ||
			train.carriages.last().trailingPoint.edge?.isTurn == true
		) {
			topSpeed = train.maxTurnSpeed().toDouble()
		}
		
		topSpeed = min(topSpeed, cappedTopSpeed)
		
		train.targetSpeed = when(throttle.reverser) {
			Throttles.Reverser.Forward -> topSpeed
			Throttles.Reverser.Neutral -> 0.0
			Throttles.Reverser.Backward -> -topSpeed
		}
		
		val accelerationMod = abs(throttle.acceleration).toFloat()
		train.approachTargetSpeed(accelerationMod)
	}
	
	private fun getOrCreateContext(
		world: LevelAccessor,
		playerId: UUID,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
	): ServerThrottleControl {
		val worldThrottles = receivedControls[world]
		worldThrottles.byPlayer[playerId]?.let {
			if(it.entity != entity) worldThrottles.removeControl(it)
		}
		
		val context = worldThrottles.getOrPutControl(playerId = playerId) {
			ServerThrottleControl(playerId, entity, controlsPos)
		}
		context.updateControlsPos(controlsPos)
		return context
	}
	
	fun receiveControl(
		world: LevelAccessor,
		entity: CarriageContraptionEntity,
		controlsPos: BlockPos,
		playerId: UUID,
		relativeThrottle: Throttles.Throttle,
		keys: Collection<Int>,
	) {
		val context = getOrCreateContext(world, playerId, entity, controlsPos)
		context.updateControlsPos(controlsPos)
		context.relativeThrottle = relativeThrottle
		
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
		PacketDistributor.sendToPlayer(player, UpdateThrottlePacket(context))
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


open class ServerThrottleTrain(val train: Train) {
	val trainId: UUID
		get() = train.id
	
	var throttle: Throttles.Throttle = train.absoluteThrottle
}

class ServerThrottleControl(
	val playerId: UUID,
	val entity: CarriageContraptionEntity,
	controlsPos: BlockPos,
) : ServerThrottleTrain(train = entity.carriage.train) {
	var controlsPos: BlockPos = controlsPos
		private set
	
	val forward: Boolean
		get() {
			val info = entity.contraption.blocks[controlsPos] ?: return true
			if(!info.state.hasProperty(ControlsBlock.FACING)) return true
			
			return info.state.getValue(ControlsBlock.FACING) == entity.initialOrientation.counterClockWise
		}
	
	var relativeThrottle: Throttles.Throttle
		get() = throttle.reverseIf(forward)
		set(value) {
			throttle = value.reverseIf(forward)
		}
	
	val keys = mutableSetOf<ManuallyPressedKey>()
	
	fun updateControlsPos(pos: BlockPos) {
		if(controlsPos == pos) return
		
		controlsPos = pos
	}
	
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
