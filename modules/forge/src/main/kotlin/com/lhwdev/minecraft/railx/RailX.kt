package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.lhwdev.minecraft.railx.registry.AllCreativeModeTabs
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.AllTags
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.runWhenOn

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
		
		fun asResource(path: String): ResourceLocation =
			ResourceLocation.fromNamespaceAndPath(Id, path)
	}
	
	init {
		container.registerConfig(ModConfig.Type.SERVER, RailXConfig.Server.spec)
		
		runWhenOn(Dist.CLIENT) {
			container.registerExtensionPoint(
				IConfigScreenFactory::class.java,
				IConfigScreenFactory { container, screen -> ConfigurationScreen(container, screen) }
			)
		}
		
		// ensures initialization of registry
		AllTags
		AllBlocks
		AllBlockEntityTypes
		AllCreativeModeTabs
		AllPackets.register()
	}
}
