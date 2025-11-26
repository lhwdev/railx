package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.registry.AllKeys
import com.lhwdev.minecraft.railx.utils.sign
import com.mojang.blaze3d.platform.InputConstants
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket
import com.simibubi.create.foundation.utility.ControlsUtil
import net.minecraft.client.Minecraft
import net.minecraft.world.level.LevelAccessor
import net.minecraftforge.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW


object ThrottlesClient {
	var throttle: Throttles.Throttle? = null
	var packetCooldown = 0
	
	init {
		ControlsHandler.currentlyPressed = mutableListOf()
	}
	
	
	fun levelUnloaded(level: LevelAccessor) {
		packetCooldown = 0
		throttle = null
	}
	
	fun startControlling() {
		throttle = Throttles.Throttle.Neutral
	}
	
	fun stopControlling() {
		throttle = null
	}
	
	fun tick() {
		val mc = Minecraft.getInstance()
		val entity = ControlsHandler.getContraption() ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		if(packetCooldown > 0) packetCooldown--
		if(entity.isRemoved || InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_ESCAPE)) {
			ControlsHandler.stopControlling()
			PacketDistributor.sendToServer(
				ControlsInputPacket(ControlsHandler.currentlyPressed, false, entity.id, controlsPos, true)
			)
			return
		}
		
		val previous = throttle
		
		val controls = ControlsUtil.getControls()
		val pressedKeys = controls
			.mapIndexedNotNull { index, control -> index.takeIf { ControlsUtil.isActuallyPressed(control) } }
		
		var reverser = previous?.reverser ?: Throttles.Reverser.Neutral
		if(AllKeys.ThrottleReverserForward.isKeyDown) reverser = reverser.forward()
		if(AllKeys.ThrottleReverserBackward.isKeyDown) reverser = reverser.backward()
		
		val steering = when {
			2 in pressedKeys -> Throttles.Steering.Left
			3 in pressedKeys -> Throttles.Steering.Right
			else -> Throttles.Steering.Neutral
		}
		
		// TODO: hold long to move more
		var gear = previous?.gear ?: 0
		if(AllKeys.ThrottleAccelerate.isKeyDown) gear = (gear + 1).coerceAtMost(7)
		if(AllKeys.ThrottleNeutral.isKeyDown) gear += -sign(gear)
		if(AllKeys.ThrottleBrake.isKeyDown) gear = (gear - 1).coerceAtLeast(-4)
		
		val throttle = Throttles.Throttle(reverser, steering, gear)
		if(pressedKeys != ControlsHandler.currentlyPressed || throttle != previous || packetCooldown == 0) {
			ControlsHandler.currentlyPressed = pressedKeys
			this.throttle = throttle
			val packet = ThrottlePacket(
				contraptionEntityId = entity.id,
				controlsPos = controlsPos,
				throttle = throttle,
				otherKeys = pressedKeys,
			)
			PacketDistributor.sendToServer(packet)
		}
		
		controls.forEach { it.isDown = false }
	}
}
