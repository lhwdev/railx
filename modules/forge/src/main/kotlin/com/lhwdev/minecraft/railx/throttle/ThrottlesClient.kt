package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.AllKeys
import com.lhwdev.minecraft.railx.utils.KeyDebounce
import com.lhwdev.minecraft.railx.utils.orDefault
import com.lhwdev.minecraft.railx.utils.sign
import com.mojang.blaze3d.platform.InputConstants
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket
import com.simibubi.create.foundation.utility.ControlsUtil
import net.minecraft.client.Minecraft
import net.minecraft.world.level.LevelAccessor
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW


@OnlyIn(Dist.CLIENT)
object ThrottlesClient {
	var throttle: Throttles.Throttle? = null
	var packetCooldown = 0
	
	private val accelerateKey = KeyDebounce.HoldRepeat(key = AllKeys.ThrottleAccelerate)
	private val neutralKey = KeyDebounce.HoldRepeat(key = AllKeys.ThrottleNeutral)
	private val brakeKey = KeyDebounce.HoldRepeat(key = AllKeys.ThrottleBrake)
	private val emergencyBrakeKey = KeyDebounce.Hold(key = AllKeys.ThrottleEmergencyBrake)
	
	private val keyGroup = KeyDebounce.DebounceGroup(emergencyBrakeKey, brakeKey, neutralKey, accelerateKey)
	
	private val emergencyBrakeGear: Int
		get() = if(RailXConfig.Server.throttle.emergencyBrakeMultiplier.orDefault(1.0) != 1.0)
			-Throttles.maxBreak - 1
		else -Throttles.maxBreak
	
	
	fun levelUnloaded(level: LevelAccessor) {
		packetCooldown = 0
		throttle = null
		keyGroup.resetAll()
	}
	
	fun startControlling() {
		val contraptionEntityId = ControlsHandler.getContraption()?.id ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		
		PacketDistributor.sendToServer(StartControllingPacket(contraptionEntityId, controlsPos))
	}
	
	fun stopControlling() {
		if(throttle != null) {
			// as ControlsHandler.currentlyPressed is always empty when throttle feature is on
			val entity = ControlsHandler.getContraption()
			val controlsPos = ControlsHandler.getControlsPos()
			if(entity != null && controlsPos != null)
				PacketDistributor.sendToServer(ControlsInputPacket(emptyList(), false, entity.id, controlsPos, false))
		}
		
		throttle = null
		packetCooldown = 0
		keyGroup.resetAll()
	}
	
	fun tick() {
		val mc = Minecraft.getInstance()
		val entity = ControlsHandler.getContraption() ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		if(packetCooldown > 0) packetCooldown--
		if(entity.isRemoved || InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_ESCAPE)) {
			ControlsHandler.stopControlling()
			PacketDistributor.sendToServer(
				ThrottlePacket(
					contraptionEntityId = entity.id,
					controlsPos = controlsPos,
					throttle = Throttles.Throttle.Neutral,
					keys = ControlsHandler.currentlyPressed.toList(),
					stopControlling = true,
				)
			)
			return
		}
		
		val previous = throttle ?: return
		
		val controls = ControlsUtil.getControls()
		val pressedKeys = controls
			.mapIndexedNotNull { index, control -> index.takeIf { ControlsUtil.isActuallyPressed(control) } }
		
		var reverser = previous.reverser
		var gear = previous.gear
		
		if(gear <= 0) {
			if(AllKeys.ThrottleReverserForward.isKeyDown) reverser = reverser.forward()
			if(AllKeys.ThrottleReverserBackward.isKeyDown) reverser = reverser.backward()
		}
		
		val steering = when {
			2 in pressedKeys -> Throttles.Steering.Left
			3 in pressedKeys -> Throttles.Steering.Right
			else -> Throttles.Steering.Neutral
		}
		
		keyGroup.tickPressed()
		
		if(emergencyBrakeKey.isPressed) {
			gear = emergencyBrakeGear
			keyGroup.resetAll()
		} else {
			if(accelerateKey.isPressed)
				gear = (gear + 1).coerceAtMost(Throttles.maxThrottle)
			
			if(neutralKey.isPressed)
				gear += -sign(gear)
			
			if(brakeKey.isPressed)
				gear = (gear - 1).coerceAtLeast(emergencyBrakeGear)
		}
		
		if(reverser == Throttles.Reverser.Neutral && gear > 0) gear = 0
		
		val throttle = Throttles.Throttle(reverser, steering, gear)
		// A changed gear must reach the server on the tick it changes; packetCooldown only limits keepalives.
		if(pressedKeys != ControlsHandler.currentlyPressed || throttle != previous || packetCooldown == 0) {
			ControlsHandler.currentlyPressed = pressedKeys
			this.throttle = throttle
			val packet = ThrottlePacket(
				contraptionEntityId = entity.id,
				controlsPos = controlsPos,
				throttle = throttle,
				keys = pressedKeys,
			)
			PacketDistributor.sendToServer(packet)
			packetCooldown = ControlsHandler.PACKET_RATE
		}
		
		controls.forEach { it.isDown = false }
	}
}
