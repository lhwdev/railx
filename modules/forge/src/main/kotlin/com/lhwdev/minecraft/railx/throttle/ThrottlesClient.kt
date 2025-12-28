package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.registry.AllKeys
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.utils.sign
import com.mojang.blaze3d.platform.InputConstants
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket
import com.simibubi.create.foundation.utility.ControlsUtil
import net.minecraft.client.Minecraft
import net.minecraft.world.level.LevelAccessor
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.glfw.GLFW
import com.simibubi.create.AllPackets as CreatePackets

@OnlyIn(Dist.CLIENT)
object ThrottlesClient {
	var throttle: Throttles.Throttle? = null
	var packetCooldown = 0
	
	
	fun levelUnloaded(level: LevelAccessor) {
		packetCooldown = 0
		throttle = null
	}
	
	fun startControlling() {
		throttle = Throttles.Throttle.Neutral
	}
	
	fun stopControlling() {
		if(throttle != null) {
			// as ControlsHandler.currentlyPressed is always empty when throttle feature is on
			val entity = ControlsHandler.getContraption()
			val controlsPos = ControlsHandler.getControlsPos()
			if(entity != null && controlsPos != null)
				CreatePackets.getChannel().sendToServer(ControlsInputPacket(emptyList(), false, entity.id, controlsPos, false))
		}
		
		throttle = null
		packetCooldown = 0
	}
	
	fun tick() {
		val mc = Minecraft.getInstance()
		val entity = ControlsHandler.getContraption() ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		if(packetCooldown > 0) packetCooldown--
		if(entity.isRemoved || InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_ESCAPE)) {
			ControlsHandler.stopControlling()
			AllPackets.sendToServer(
				ThrottlePacket(
					contraptionEntityId = entity.id,
					controlsPos = controlsPos,
					throttle = Throttles.Throttle.Neutral,
					otherKeys = ControlsHandler.currentlyPressed.toList(),
					stopControlling = true,
				)
			)
			return
		}
		
		val previous = throttle
		
		val controls = ControlsUtil.getControls()
		val pressedKeys = controls
			.mapIndexedNotNull { index, control -> index.takeIf { ControlsUtil.isActuallyPressed(control) } }
		
		var reverser = previous?.reverser ?: Throttles.Reverser.Neutral
		var gear = previous?.gear ?: 0
		
		if(gear <= 0) {
			if(AllKeys.ThrottleReverserForward.isKeyDown) reverser = reverser.forward()
			if(AllKeys.ThrottleReverserBackward.isKeyDown) reverser = reverser.backward()
		}
		
		val steering = when {
			2 in pressedKeys -> Throttles.Steering.Left
			3 in pressedKeys -> Throttles.Steering.Right
			else -> Throttles.Steering.Neutral
		}
		
		// TODO: hold long to move more
		if(AllKeys.ThrottleAccelerate.isKeyDown) gear = (gear + 1).coerceAtMost(Throttles.maxThrottle)
		if(AllKeys.ThrottleNeutral.isKeyDown) gear += -sign(gear)
		if(AllKeys.ThrottleBrake.isKeyDown) gear = (gear - 1).coerceAtLeast(-Throttles.maxBreak)
		if(reverser == Throttles.Reverser.Neutral && gear > 0) gear = 0
		
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
			AllPackets.sendToServer(packet)
			packetCooldown = ControlsHandler.PACKET_RATE
		}
		
		controls.forEach { it.isDown = false }
	}
}
