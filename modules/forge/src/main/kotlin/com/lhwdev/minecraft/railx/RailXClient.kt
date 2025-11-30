@file:Suppress("unused")

package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.registry.RailXPonderPlugin
import net.createmod.ponder.foundation.PonderIndex
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS


// TODO: multiple @Mod class not supported in KotlinForForge
//   - see https://github.com/thedarkcolour/KotlinForForge/issues/142
// @Mod(value = RailX.Id, dist = [Dist.CLIENT])
class RailXClient {
	init {
		val bus = MOD_BUS
		bus.addListener(::clientInit)
	}
	
	
	fun clientInit(event: FMLClientSetupEvent) {
		PonderIndex.addPlugin(RailXPonderPlugin())
	}
}
