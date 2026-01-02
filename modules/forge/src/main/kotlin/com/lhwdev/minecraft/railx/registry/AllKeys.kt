package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import org.lwjgl.glfw.GLFW

@OnlyIn(Dist.CLIENT)
enum class AllKeys(val description: String, val defaultKey: Int) {
	FlexiblePlacement(description = "flexiTrack.flexible_placement", defaultKey = GLFW.GLFW_KEY_LEFT_CONTROL),
	
	ThrottleAccelerate(description = "throttle.accelerate", defaultKey = GLFW.GLFW_KEY_LEFT_BRACKET),
	ThrottleNeutral(description = "throttle.neutral", defaultKey = GLFW.GLFW_KEY_SEMICOLON),
	ThrottleBrake(description = "throttle.brake", defaultKey = GLFW.GLFW_KEY_PERIOD),
	ThrottleReverserForward(description = "throttle.reverser_forward", defaultKey = GLFW.GLFW_KEY_O),
	ThrottleReverserBackward(description = "throttle.reverser_backward", defaultKey = GLFW.GLFW_KEY_K),
	;
	
	var bound: KeyMapping? = null
		private set
	
	val key: Int
		get() = bound?.key?.value ?: defaultKey
	
	var isPressed: Boolean = false
		private set
	
	var isKeyDown: Boolean = false
		private set
	
	var isKeyUp: Boolean = false
		private set
	
	var pressedTicks: Int = 0
		private set
	
	private fun updatePressed() {
		val previous = isPressed
		val current = bound?.isDown ?: isKeyDown(defaultKey)
		isPressed = current
		
		isKeyDown = !previous && current
		isKeyUp = previous && !current
		
		if(isKeyDown) pressedTicks = 0
		else if(current) pressedTicks++
	}
	
	
	@EventBusSubscriber(Dist.CLIENT)
	companion object {
		@SubscribeEvent
		fun register(event: RegisterKeyMappingsEvent) {
			for(key in entries) {
				val mapping = KeyMapping(key.description, key.defaultKey, RailX.Name)
				key.bound = mapping
				event.register(mapping)
			}
		}
		
		@SubscribeEvent(priority = EventPriority.HIGHEST)
		private fun onTick(event: ClientTickEvent.Pre) {
			for(key in AllKeys.entries) {
				key.updatePressed()
			}
		}
		
		fun isKeyDown(key: Int): Boolean =
			InputConstants.isKeyDown(Minecraft.getInstance().window.window, key)
	}
}
