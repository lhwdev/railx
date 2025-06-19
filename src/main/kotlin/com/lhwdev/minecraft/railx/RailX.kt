package com.lhwdev.minecraft.railx

import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/**
 * Main mod class.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(RailX.Id)
class RailX(container: ModContainer) {
	companion object {
		const val Id = "railx"
		
		val Logger: Logger = LogManager.getLogger(Id)
	}
	
	init {
		val bus = MOD_BUS
		
		ModBlocks.Registry.register(bus)
		
		container.registerConfig(ModConfig.Type.SERVER, RailXConfig.Server.spec)
		container.registerExtensionPoint(
			IConfigScreenFactory::class.java,
			IConfigScreenFactory { container, screen -> ConfigurationScreen(container, screen) }
		)
	}
}
