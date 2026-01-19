package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.throttle.ThrottlesServer
import com.simibubi.create.Create
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import com.simibubi.create.content.trains.entity.Train
import net.createmod.catnip.data.WorldAttached
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.LevelAccessor
import java.lang.invoke.MethodHandles
import java.util.*


object RealisticSpeedControl {
	val controls: WorldAttached<Controls> = WorldAttached { Controls() }
	
	class Controls {
		val byPlayer = mutableMapOf<UUID, Control>()
		val byTrain = mutableMapOf<UUID, Control>()
		
		fun getOrPut(world: LevelAccessor, playerId: UUID, trainId: UUID): Control =
			byPlayer[playerId] ?: Control(
				player = world.getPlayerByUUID(playerId) as ServerPlayer,
				train = Create.RAILWAYS.trains[trainId]!!,
			).also {
				byPlayer[playerId] = it
				byTrain[playerId] = it
			}
	}
	
	class Control(val player: ServerPlayer, val train: Train) {
		var parameters: RealisticSpeedParameters? = null
		var packetCooldown = 0
	}
	
	
	fun tick(world: LevelAccessor) {
		val controls = controls[world]
		
		val players = mutableSetOf<UUID>()
		
		for((playerId, throttle) in ThrottlesServer.receivedThrottles[world].byPlayer) {
			players += playerId
			val control = controls.getOrPut(world, playerId, trainId = throttle.trainId)
			updateForPlayer(control)
		}
		
		for((playerId, control) in ControlsServerHandler.receivedInputs[world] as Map<UUID, Any>) {
			if(playerId in players) continue
			players += playerId
			val entity = Reflection.entity.invoke(control) as? CarriageContraptionEntity ?: continue
			val control = controls.getOrPut(world, playerId, trainId = entity.carriage.train.id)
			updateForPlayer(control)
		}
		
		val iterator = controls.byPlayer.iterator()
		while(iterator.hasNext()) {
			val (playerId, control) = iterator.next()
			if(playerId !in players) {
				iterator.remove()
				controls.byTrain.remove(control.train.id)
			}
		}
	}
	
	private fun updateForPlayer(control: Control) {
		val parameters = RealisticSpeedParameters(control)
		if(parameters != control.parameters || control.packetCooldown <= 0) {
			if(parameters != null)
				AllPackets.sendToPlayer(control.player, UpdateRealisticPacket(parameters))
			control.parameters = parameters
			control.packetCooldown = 100
		} else {
			control.packetCooldown--
		}
	}
	
	
	private object Reflection {
		val entity = Class.forName("${ControlsServerHandler::class.java.name}\$ControlsContext")
			.getDeclaredField("entity")
			.also { it.isAccessible = true }
			.let { MethodHandles.lookup().unreflectGetter(it) }
		
	}
}
