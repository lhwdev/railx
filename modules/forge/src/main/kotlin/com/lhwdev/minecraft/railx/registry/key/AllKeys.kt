package com.lhwdev.minecraft.railx.registry.key

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.utils.Registration
import net.minecraft.client.KeyMapping
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import org.lwjgl.glfw.GLFW


@OnlyIn(Dist.CLIENT)
enum class AllKeys(val description: String, val defaultKey: Int) {
	FlexiblePlacement(description = "flexiTrack.flexible_placement", defaultKey = GLFW.GLFW_KEY_LEFT_CONTROL),
	
	ThrottleAccelerate(description = "throttle.accelerate", defaultKey = GLFW.GLFW_KEY_LEFT_BRACKET),
	ThrottleNeutral(description = "throttle.neutral", defaultKey = GLFW.GLFW_KEY_SEMICOLON),
	ThrottleBrake(description = "throttle.brake", defaultKey = GLFW.GLFW_KEY_PERIOD),
	ThrottleEmergencyBrake(description = "throttle.emergency_brake", defaultKey = GLFW.GLFW_KEY_COMMA),
	ThrottleReverserForward(description = "throttle.reverser_forward", defaultKey = GLFW.GLFW_KEY_O),
	ThrottleReverserBackward(description = "throttle.reverser_backward", defaultKey = GLFW.GLFW_KEY_K),
	;
	
	var bound: KeyMapping? = null
		private set
	
	val key: Int
		get() = bound?.key?.value ?: defaultKey
	
	
	var isDown: Boolean = false
		private set
	
	private val handlers = mutableSetOf<KeyEventListener>()
	
	fun onKeyDown(handler: KeyEventListener.Down): Registration =
		onKeyUpdate { if(it.isDown) handler.onDown(it) }
	
	fun onKeyUp(handler: KeyEventListener.Up): Registration =
		onKeyUpdate { if(!it.isDown) handler.onUp(it) }
	
	fun onKey(handler: KeyEventListener): Registration =
		onKeyUpdate { handler.onUpdate(it) }
	
	private fun onKeyUpdate(handler: KeyEventListener): Registration {
		handlers.add(handler)
		return { handlers.remove(handler) }
	}
	
	
	@EventBusSubscriber(Dist.CLIENT)
	companion object {
		private val globalHandlers = mutableSetOf<KeyEventListener>()
		
		fun onGlobalKeyDown(handler: KeyEventListener.Down): Registration =
			onGlobalKeyUpdate { if(it.isDown) handler.onDown(it) }
		
		fun onGlobalKeyUp(handler: KeyEventListener.Up): Registration =
			onGlobalKeyUpdate { if(it.isDown) handler.onUp(it) }
		
		fun onGlobalKey(handler: KeyEventListener): Registration =
			onGlobalKeyUpdate { handler.onUpdate(it) }
		
		private fun onGlobalKeyUpdate(handler: KeyEventListener): Registration {
			globalHandlers.add(handler)
			return { globalHandlers.remove(handler) }
		}
		
		
		@SubscribeEvent
		fun onRegisterKeys(event: RegisterKeyMappingsEvent) {
			for(key in entries) {
				val mapping = KeyMapping(key.description, key.defaultKey, RailX.Name)
				key.bound = mapping
				event.register(mapping)
			}
		}
		
		@SubscribeEvent
		fun onKeyInput(event: InputEvent.Key) {
			if(event.action != GLFW.GLFW_PRESS && event.action != GLFW.GLFW_RELEASE) return
			
			val isDown = event.action == GLFW.GLFW_PRESS
			for(key in entries) {
				if(key.key != event.key) continue
				if(key.isDown != isDown) {
					key.isDown = isDown
					globalHandlers.forEach { it.onUpdate(key) }
					key.handlers.forEach { it.onUpdate(key) }
				}
			}
		}
	}
}
