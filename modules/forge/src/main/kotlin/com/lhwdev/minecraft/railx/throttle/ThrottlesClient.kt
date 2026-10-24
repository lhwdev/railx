package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.AllKeys
import com.lhwdev.minecraft.railx.utils.KeyDebounce
import com.lhwdev.minecraft.railx.utils.orDefault
import com.lhwdev.minecraft.railx.utils.sign
import com.mojang.blaze3d.platform.InputConstants
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket
import com.simibubi.create.foundation.utility.ControlsUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.util.Mth
import net.minecraft.world.level.LevelAccessor
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW
import kotlin.math.abs


@OnlyIn(Dist.CLIENT)
object ThrottlesClient {
	var throttle: Throttles.Throttle? = null
	var packetCooldown = 0
	
	private val reverserForwardKey = KeyDebounce.Hold(key = AllKeys.ThrottleReverserForward)
	private val reverserBackwardKey = KeyDebounce.Hold(key = AllKeys.ThrottleReverserBackward)
	
	private val accelerateKey = KeyDebounce.HoldRepeatWithBoost(key = AllKeys.ThrottleAccelerate)
	private val neutralKey = KeyDebounce.HoldRepeatWithBoost(key = AllKeys.ThrottleNeutral)
	private val brakeKey = KeyDebounce.HoldRepeatWithBoost(key = AllKeys.ThrottleBrake)
	private val emergencyBrakeKey = KeyDebounce.Hold(key = AllKeys.ThrottleEmergencyBrake)
	private var emergencyBrakeSoundTicks = 0
	private var emergencyBreakSoundRange = 0f..0f
	
	private val reverserGroup = KeyDebounce.DebounceGroup(reverserForwardKey, reverserBackwardKey)
	private val throttleNotchGroup = KeyDebounce.DebounceGroup(emergencyBrakeKey, brakeKey, neutralKey, accelerateKey)
	private val keyGroup = KeyDebounce.Group(reverserGroup, throttleNotchGroup)
	
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
		
		keyGroup.tickPressed()
		
		if(gear <= 0) {
			if(reverserForwardKey.isPressed) reverser = reverser.forward()
			if(reverserBackwardKey.isPressed) reverser = reverser.backward()
		}
		
		val steering = when {
			2 in pressedKeys -> Throttles.Steering.Left
			3 in pressedKeys -> Throttles.Steering.Right
			else -> Throttles.Steering.Neutral
		}
		
		val previousGear = gear
		val emergencyBrakeGear = emergencyBrakeGear
		if(emergencyBrakeKey.isPressed) {
			gear = emergencyBrakeGear
		} else {
			if(accelerateKey.isPressed)
				gear = (gear + 1).coerceAtMost(Throttles.maxThrottle)
			
			if(neutralKey.isPressed)
				gear += -sign(gear)
			
			if(brakeKey.isPressed)
				gear = (gear - 1).coerceAtLeast(emergencyBrakeGear)
		}
		
		if(reverser == Throttles.Reverser.Neutral && gear > 0) gear = 0
		
		fun pitchOf(gear: Int): Float {
			val pitch = (gear + -emergencyBrakeGear).toFloat() / (-emergencyBrakeGear + Throttles.maxThrottle)
			return Mth.lerp(pitch, 1.15f, 1.4f /* 1.5f */)
		}
		
		fun playSound(pitch: Float) {
			Minecraft.getInstance().soundManager
				.play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.mainEvent, pitch, 0.25f))
		}
		
		val MaxEmergencyGearSound = 4
		if(throttleNotchGroup.isPressed && gear != previousGear) {
			if(emergencyBrakeKey.isPressed) {
				val delta = gear - previousGear
				if(abs(delta) <= MaxEmergencyGearSound) {
					for(gearStep in previousGear downTo gear) {
						playSound(pitch = pitchOf(gearStep))
					}
				} else {
					emergencyBreakSoundRange = pitchOf(previousGear)..pitchOf(gear)
					emergencyBrakeSoundTicks = MaxEmergencyGearSound
				}
			} else {
				playSound(pitch = pitchOf(gear))
				emergencyBrakeSoundTicks = 0
			}
		}
		
		if(emergencyBrakeSoundTicks > 0) {
			emergencyBrakeSoundTicks--
			
			val pitch = Mth.lerp(
				(emergencyBrakeSoundTicks).toFloat() / MaxEmergencyGearSound,
				emergencyBreakSoundRange.start,
				emergencyBreakSoundRange.endInclusive
			)
			playSound(pitch)
		}
		
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
