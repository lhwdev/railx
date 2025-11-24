package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import org.lwjgl.glfw.GLFW


enum class AllKeys(val description: String, val defaultKey: Int) {
	ThrottleAccelerate(description = "throttle.accelerate", defaultKey = GLFW.GLFW_KEY_LEFT_BRACKET),
	ThrottleNeutral(description = "throttle.neutral", defaultKey = GLFW.GLFW_KEY_SEMICOLON),
	ThrottleBrake(description = "throttle.brake", defaultKey = GLFW.GLFW_KEY_SLASH),
	;
	
	var bound: KeyMapping? = null
		private set
	
	val key: Int
		get() = bound?.key?.value ?: defaultKey
	
	val isPressed: Boolean
		get() = bound?.isDown ?: isKeyDown(defaultKey)
	
	
	@EventBusSubscriber
	companion object {
		@SubscribeEvent
		private fun register(event: RegisterKeyMappingsEvent) {
			for(key in entries) {
				val mapping = KeyMapping(key.description, key.defaultKey, RailX.Name)
				key.bound = mapping
				event.register(mapping)
			}
		}
		
		fun isKeyDown(key: Int): Boolean =
			InputConstants.isKeyDown(Minecraft.getInstance().window.window, key)
	}
}
