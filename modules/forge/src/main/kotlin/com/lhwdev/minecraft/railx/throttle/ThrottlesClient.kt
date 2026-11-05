package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.key.AllKeys
import com.lhwdev.minecraft.railx.registry.key.KeyDebounce
import com.lhwdev.minecraft.railx.registry.key.KeyDebounceHandler
import com.lhwdev.minecraft.railx.registry.key.registerGlobalKeyEvent
import com.lhwdev.minecraft.railx.utils.getOrDefault
import com.lhwdev.minecraft.railx.utils.orDefault
import com.lhwdev.minecraft.railx.utils.sign
import com.mojang.blaze3d.platform.InputConstants
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket
import com.simibubi.create.foundation.utility.ControlsUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.LevelAccessor
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant


@OnlyIn(Dist.CLIENT)
object ThrottlesClient {
	var throttle: Throttles.Throttle? = null
		private set
	
	private var packetCooldown = 0
	
	val keys = ThrottleKeys()
	private var lastEmergencyBreakPressed = false
	
	init {
		keys.registerGlobalKeyEvent()
	}
	
	
	private val emergencyBrakeGear: Int
		get() = if(RailXConfig.Server.throttle.emergencyBrakeMultiplier.orDefault(1.0) != 1.0)
			-Throttles.maxBreak - 1
		else -Throttles.maxBreak
	
	private const val MaxEmergencyBrakeTicks = 5
	
	
	fun levelUnloaded(level: LevelAccessor) {
		packetCooldown = 0
		throttle = null
		keys.reset()
	}
	
	
	fun receiveThrottle(contraptionEntityId: Int, value: Throttles.Throttle) {
		val mc = Minecraft.getInstance()
		if(!mc.isSameThread) {
			mc.execute { receiveThrottle(contraptionEntityId, value) }
			return
		}
		
		if(ControlsHandler.getContraption()?.id != contraptionEntityId) return
		keys.reset()
		throttle = value
	}
	
	fun canHandleKeys(): Boolean {
		if(!RailXConfig.Server.throttle.enabled.getOrDefault(false)) return false
		
		val mc = Minecraft.getInstance()
		return throttle != null &&
			ControlsHandler.getContraption()?.isRemoved == false &&
			ControlsHandler.getControlsPos() != null &&
			mc.isWindowActive &&
			mc.screen == null &&
			mc.overlay == null
	}
	
	
	fun startControlling() {
		keys.reset()
		
		throttle = null
		packetCooldown = 0
		
		val contraptionEntityId = ControlsHandler.getContraption()?.id ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		
		val packet = StartControllingPacket(contraptionEntityId, controlsPos)
		PacketDistributor.sendToServer(packet)
	}
	
	fun stopControlling() {
		if(throttle != null) {
			val entity = ControlsHandler.getContraption()
			val controlsPos = ControlsHandler.getControlsPos()
			if(entity != null && controlsPos != null) {
				val packet = ControlsInputPacket(emptyList(), false, entity.id, controlsPos, false)
				PacketDistributor.sendToServer(packet)
			}
		}
		
		throttle = null
		packetCooldown = 0
		keys.reset()
	}
	
	fun tick() {
		val mc = Minecraft.getInstance()
		val entity = ControlsHandler.getContraption() ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		
		if(packetCooldown > 0) packetCooldown--
		
		if(entity.isRemoved || InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_ESCAPE)) {
			ControlsHandler.stopControlling()
			
			val packet = ThrottlePacket(
				contraptionEntityId = entity.id,
				controlsPos = controlsPos,
				throttle = Throttles.Throttle.Neutral,
				keys = ControlsHandler.currentlyPressed.toList(),
				stopControlling = true,
			)
			
			PacketDistributor.sendToServer(packet)
			return
		}
		
		updateKeys(ticking = true)
		val previous = throttle ?: return
		
		val controls = ControlsUtil.getControls()
		val pressedKeys = controls
			.mapIndexedNotNull { index, control -> index.takeIf { ControlsUtil.isActuallyPressed(control) } }
		
		if(!canHandleKeys()) keys.reset()
		
		val steering = when {
			2 in pressedKeys -> Throttles.Steering.Left
			3 in pressedKeys -> Throttles.Steering.Right
			else -> Throttles.Steering.Neutral
		}
		val value = Throttles.Throttle(previous.reverser, steering, previous.gear)
		val changed = value != previous || pressedKeys != ControlsHandler.currentlyPressed
		
		ControlsHandler.currentlyPressed = pressedKeys
		throttle = value
		
		if(changed || packetCooldown == 0) sendThrottle()
		controls.forEach { it.isDown = false }
	}
	
	fun updateKeys(ticking: Boolean = false) {
		try {
			update(keys = keys, ticking = ticking)
		} finally {
			keys.consumeAll()
		}
	}
	
	private fun update(keys: ThrottleKeys, ticking: Boolean) {
		if(!canHandleKeys()) return
		val previous = throttle ?: return
		var reverser = previous.reverser
		var gear = previous.gear
		
		if(gear <= 0) {
			if(keys.reverserForward.isPressed) reverser = reverser.forward()
			if(keys.reverserBackward.isPressed) reverser = reverser.backward()
		}
		
		displayEmergencyBrakeHints()
		
		val emergencyBrakeGear = emergencyBrakeGear
		if(keys.emergencyBrake.isPressed) {
			if(ticking) {
				val range = Throttles.maxThrottle - emergencyBrakeGear
				val step = ((range + MaxEmergencyBrakeTicks - 1) / MaxEmergencyBrakeTicks).coerceAtLeast(1)
				gear = (gear - step).coerceAtLeast(emergencyBrakeGear)
				lastEmergencyBreakPressed = true
			}
		} else {
			if(keys.throttleAccelerate.isPressed)
				gear = (gear + 1).coerceAtMost(Throttles.maxThrottle)
			
			if(keys.throttleNeutral.isPressed)
				gear += -sign(gear)
			
			if(keys.throttleBrake.isPressed)
				gear = (gear - 1).coerceAtLeast(emergencyBrakeGear)
			
			if(ticking) lastEmergencyBreakPressed = false
		}
		
		if(reverser == Throttles.Reverser.Neutral && gear > 0) gear = 0
		
		val new = Throttles.Throttle(reverser, previous.steering, gear)
		playSound(new)
		
		if(new != previous) {
			throttle = new
			sendThrottle()
		}
	}
	
	private fun playSound(newThrottle: Throttles.Throttle) {
		val throttle = throttle ?: return
		
		val reverser = newThrottle.reverser
		val gear = newThrottle.gear
		val previousReverser = throttle.reverser
		val previousGear = throttle.gear
		
		fun pitchOf(gear: Int): Float {
			val pitch = (gear + -emergencyBrakeGear).toFloat() / (-emergencyBrakeGear + Throttles.maxThrottle)
			return Mth.lerp(pitch, 1.15f, 1.4f)
		}
		
		fun playSound(pitch: Float) {
			val sound = SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.mainEvent, pitch, 0.25f)
			
			Minecraft.getInstance()
				.soundManager.play(sound)
		}
		
		if(reverser != previousReverser) {
			playSound(pitch = if(reverser == Throttles.Reverser.Neutral) 0.76f else 0.78f)
		}
		
		if(gear != previousGear) {
			playSound(pitch = pitchOf(gear))
		}
	}
	
	private fun displayEmergencyBrakeHints() {
		val player = Minecraft.getInstance().player ?: return
		
		if(lastEmergencyBreakPressed && !keys.emergencyBrake.isPressed && throttle?.gear != emergencyBrakeGear) {
			val message = Component.literal("Hold [")
				.append(Component.keybind(AllKeys.ThrottleEmergencyBrake.description))
				.append("] Long to activate Emergency Brake")
			player.displayClientMessage(message, true)
		}
	}
	
	
	private fun sendThrottle() {
		val entity = ControlsHandler.getContraption() ?: return
		val controlsPos = ControlsHandler.getControlsPos() ?: return
		val value = throttle ?: return
		
		val packet = ThrottlePacket(
			contraptionEntityId = entity.id,
			controlsPos = controlsPos,
			throttle = value,
			keys = ControlsHandler.currentlyPressed.toList(),
		)
		
		PacketDistributor.sendToServer(packet)
		packetCooldown = ControlsHandler.PACKET_RATE
	}
	
	
	class ThrottleKeys : KeyDebounceHandler() {
		val reverserForward = AllKeys.ThrottleReverserForward bind KeyDebounce.Hold()
		val reverserBackward = AllKeys.ThrottleReverserBackward bind KeyDebounce.Hold()
		val reverserGroup = KeyDebounce.DebounceGroup(reverserForward, reverserBackward)
		
		val throttleAccelerate = AllKeys.ThrottleAccelerate bind KeyDebounce.HoldRepeatWithBoost()
		val throttleNeutral = AllKeys.ThrottleNeutral bind KeyDebounce.HoldRepeatWithBoost()
		val throttleBrake = AllKeys.ThrottleBrake bind KeyDebounce.HoldRepeatWithBoost()
		val emergencyBrake = AllKeys.ThrottleEmergencyBrake bind
			KeyDebounce.Delay(KeyDebounce.Repeating(), downDelay = 50.milliseconds)
		
		val throttleGroup =
			KeyDebounce.DebounceGroup(emergencyBrake, throttleBrake, throttleNeutral, throttleAccelerate)
		
		init {
			group(reverserGroup, throttleGroup)
		}
		
		
		private inline fun updater(block: () -> Unit) {
			if(!canHandleKeys()) {
				reset()
				return
			}
			block()
			updateKeys()
		}
		
		override fun onInput(key: AllKeys, now: Instant): Unit =
			updater { super.onInput(key, now) }
		
		override fun update(now: Instant): Unit =
			updater { super.update(now) }
	}
}
