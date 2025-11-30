package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrak
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrack
import com.lhwdev.minecraft.railx.registry.*
import com.lhwdev.minecraft.railx.splitGraph.SplitGraph
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * Main mod class.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(RailX.Id)
class RailX {
	companion object {
		// Note: should be const; see :api/utils.kt
		const val Id: String = "railx"
		const val Name: String = "RailX"
		
		val Logger: Logger = LogManager.getLogger(Id)
		
		lateinit var bus: IEventBus
		
		fun errorBreakpoint() {
			println("error!")
		}
		
		fun asResource(path: String): ResourceLocation =
			ResourceLocation(Id, path)
	}
	
	val bus = MOD_BUS
	
	// TODO: multiple @Mod class not supported in KotlinForForge
	//   - see https://github.com/thedarkcolour/KotlinForForge/issues/142
	private val client = RailXClient()
	
	init {
		RailX.bus = bus
		
		val context = LOADING_CONTEXT
		context.registerConfig(ModConfig.Type.COMMON, RailXConfig.Common.spec)
		context.registerConfig(ModConfig.Type.CLIENT, RailXConfig.Client.spec)
		context.registerConfig(ModConfig.Type.SERVER, RailXConfig.Server.spec)
		
		// runWhenOn(Dist.CLIENT) {
		// 	container.registerExtensionPoint(
		// 		IConfigScreenFactory::class.java,
		// 		IConfigScreenFactory { container, screen -> ConfigurationScreen(container, screen) }
		// 	)
		// }
		
		// ensures initialization of registry
		RailXRegistry.registerEventListeners(bus)
		AllBlocks.register()
		AllBlockEntityTypes.register()
		AllItems.register()
		AllCreativeModeTabs.register()
		AllCommands.register()
		AllPackets.register()
		AllEntityDataSerializers.register()
		AllTrackMaterials.register()
		AllCustoms.register()
		
		MiddleTrack.register()
		FlexiTrak.register()
		SplitGraph.register()
	}
}
