package com.lhwdev.minecraft.railx

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.config.ModConfigEvent

class Config {
	@EventBusSubscriber(modid = RailX.modId, bus = EventBusSubscriber.Bus.MOD)
	companion object {
		private val BUILDER = ForgeConfigSpec.Builder()
		val SPEC = BUILDER.build()
		
		lateinit var current: Config
		
		@SubscribeEvent
		fun onLoad(event: ModConfigEvent?) {
			current = Config(
			
			)
		}
	}
	
}
