@file:Suppress("unused")

package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.registry.RailXPonderPlugin
import net.createmod.ponder.foundation.PonderIndex
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent


// TODO: multiple @Mod class not supported in KotlinForForge
//   - see https://github.com/thedarkcolour/KotlinForForge/issues/142
// @Mod(value = RailX.Id, dist = [Dist.CLIENT])
class RailXClient(bus: IEventBus) {
	init {
		bus.addListener(::clientInit)
	}
	
	
	fun clientInit(event: FMLClientSetupEvent) {
		PonderIndex.addPlugin(RailXPonderPlugin())
	}
}
