@file:Suppress("unused")

package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.registry.RailXPonderPlugin
import net.createmod.ponder.foundation.PonderIndex
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientTickEvent


@Mod(value = RailX.Id, dist = [Dist.CLIENT])
class RailXClient(bus: IEventBus) {
	init {
		bus.addListener(::clientInit)
	}
	
	
	fun clientInit(event: FMLClientSetupEvent) {
		PonderIndex.addPlugin(RailXPonderPlugin())
	}
	
	@EventBusSubscriber(Dist.CLIENT)
	object Events {
		@SubscribeEvent
		fun onPreTick(event: ClientTickEvent.Pre) {
			// nothing
		}
		
		@SubscribeEvent
		fun onPostTick(event: ClientTickEvent.Post) {
		}
	}
}
