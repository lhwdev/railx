package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrak
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrack
import com.lhwdev.minecraft.railx.registry.*
import com.lhwdev.minecraft.railx.splitGraph.SplitGraph
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
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
class RailX(container: ModContainer, bus: IEventBus) {
	companion object {
		const val Id = "railx"
		
		val Logger: Logger = LogManager.getLogger(Id)
		
		fun errorBreakpoint() {
			println("error!")
		}
		
		fun asResource(path: String): ResourceLocation =
			ResourceLocation.fromNamespaceAndPath(Id, path)
	}
	
	// TODO: multiple @Mod class not supported in KotlinForForge
	//   - see https://github.com/thedarkcolour/KotlinForForge/issues/142
	private val client = RailXClient(bus)
	
	init {
		container.registerConfig(ModConfig.Type.COMMON, RailXConfig.Common.spec)
		container.registerConfig(ModConfig.Type.CLIENT, RailXConfig.Client.spec)
		container.registerConfig(ModConfig.Type.SERVER, RailXConfig.Server.spec)
		
		runWhenOn(Dist.CLIENT) {
			container.registerExtensionPoint(
				IConfigScreenFactory::class.java,
				IConfigScreenFactory { container, screen -> ConfigurationScreen(container, screen) }
			)
		}
		
		// ensures initialization of registry
		RailXRegistry.registerEventListeners(bus)
		AllBlocks.register()
		AllBlockEntityTypes.register()
		AllItems.register()
		AllCreativeModeTabs.register()
		AllCommands.register()
		AllPackets.register()
		AllDataComponents.register()
		AllEntityDataSerializers.register()
		AllTrackMaterials.register()
		AllCustoms.register()
		
		MiddleTrack.register()
		FlexiTrak.register()
		SplitGraph.register()
	}
}
