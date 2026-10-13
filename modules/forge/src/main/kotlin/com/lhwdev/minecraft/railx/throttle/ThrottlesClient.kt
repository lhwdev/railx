package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.AllKeys
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
	
	private val holdRepeatDelayTicks: Int
		get() = HoldRepeat.millisToTicks(RailXConfig.Client.throttle.holdRepeatDelay.asInt)
	
	private val holdRepeatIntervalTicks: Int
		get() = HoldRepeat.millisToTicks(RailXConfig.Client.throttle.holdRepeatInterval.asInt)
	
	private val accelerateHold = HoldRepeat({ holdRepeatDelayTicks }, { holdRepeatIntervalTicks })
	
	private val brakeHold = HoldRepeat({ holdRepeatDelayTicks }, { holdRepeatIntervalTicks })
	
	/** Whether the emergency-brake key was down on the previous tick, so it only wins on a fresh press. */
	private var lastEmergencyBrakePress = false
	
	/** EB is one level past the last brake notch, but only exists while the emergency brake is configured. */
	private val emergencyBrakeGear: Int
		get() = if(RailXConfig.Server.throttle.emergencyBrakeMultiplier.orDefault(1.0) != 1.0)
			-Throttles.maxBreak - 1
		else -Throttles.maxBreak
	
	
	fun levelUnloaded(level: LevelAccessor) {
		packetCooldown = 0
		throttle = null
		lastEmergencyBrakePress = false
		accelerateHold.reset()
		brakeHold.reset()
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
		lastEmergencyBrakePress = false
		accelerateHold.reset()
		brakeHold.reset()
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
		
		val emergencyBrake = RailXConfig.Server.throttle.emergencyBrakeMultiplier.asDouble
		val emergencyBrakeKey = AllKeys.ThrottleEmergencyBrake.isPressed
		
		// EB wins on the tick it goes down and holds the handle there while held; any other key then takes the
		// handle back, so the position is escapable.
		if(emergencyBrakeKey && !lastEmergencyBrakePress) {
			gear = emergencyBrakeGear
			accelerateHold.reset()
			brakeHold.reset()
		} else {
			// A held key counts as pressed repeatedly, so one press still moves one notch.
			val acceleratePressed = accelerateHold.isPressedDown(AllKeys.ThrottleAccelerate.isPressed)
			val brakePressed = brakeHold.isPressedDown(AllKeys.ThrottleBrake.isPressed)
			
			if(acceleratePressed)
				gear = (gear + 1).coerceAtMost(Throttles.maxThrottle)
			
			if(AllKeys.ThrottleNeutral.isKeyDown)
				gear += -sign(gear)
			
			if(brakePressed)
				gear = (gear - 1).coerceAtLeast(-Throttles.maxBreak - if(emergencyBrake != 1.0) 1 else 0)
		}
		lastEmergencyBrakePress = emergencyBrakeKey
		
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
