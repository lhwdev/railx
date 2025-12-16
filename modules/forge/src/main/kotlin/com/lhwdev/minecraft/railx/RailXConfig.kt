package com.lhwdev.minecraft.railx

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.*


object RailXConfig {
	sealed class Client(private val builder: Builder) {
		val common = Common()
		
		val flexiTrak = FlexiTrak()
		
		
		inner class Common {
			val preciseOverlay: BooleanValue = builder
				.comment("Displays precise information about track blocks, curves, and track placement.")
				.define("common.precise_overlay", true)
		}
		
		inner class FlexiTrak {
			val overlayWidth: IntValue = builder
				.comment("Specify how wide placement overlays are shown.")
				.defineInRange("flexi_trak.overlay_width", 3, 0, 4)
		}
		
		
		val spec: ForgeConfigSpec = builder.build()
		
		companion object Value : Client(Builder())
	}
	
	sealed class Common(private val builder: Builder) {
		val common = CommonConfig()
		
		
		inner class CommonConfig {
			val reservedSignal: BooleanValue = builder
				.comment(
					"Marks reserved signals with green color in nixie tube, instead of red. If disabled for server " +
						"side, does not displayed even if enabled for client."
				)
				.define("common.reserved_signal", false)
		}
		
		
		val spec: ForgeConfigSpec = builder.build()
		
		companion object Value : Common(Builder())
	}
	
	sealed class Server(private val builder: Builder) {
		val common = Common()
		
		val carriageMetadata = CarriageMetadata()
		
		val realisticSpeed = RealisticSpeed()
		
		val throttle = Throttle()
		
		val middleTrack = MiddleTrack()
		
		val flexiTrak = FlexiTrak()
		
		val buildTrak = BuildTrak()
		
		
		inner class Common {
			val manualStation: BooleanValue = builder
				.comment(
					"Makes approaching to station manual. Cannot use space to approach station, and when train " +
						"approached station enough, press space to mark train to be arrived at station."
				)
				.define("common.manual_station.enabled", false)
			
			val manualStationDistanceLimit: DoubleValue = builder
				.comment("Specify maximum distance between which train can arrive at station.")
				.defineInRange("common.manual_station.distance_limit", 0.5, 0.0, 10.0)
			
			val manualStationDisassembleLimit: DoubleValue = builder
				.comment("Specify maximum distance between which train can disassemble.")
				.defineInRange("common.manual_station.disassemble_limit", 0.2, 0.0, 0.5)
			
			val fakeTracksManageTickRate: IntValue = builder
				.comment("Overrides tick rate how often fake tracks are updated. Set to 0 to use default value in create, which is 100 ticks.")
				.defineInRange("common.fake_tracks.update_tick_rate", 0, 0, Int.MAX_VALUE)
			
			val noFakeTracksForUnloadedChunk: BooleanValue = builder
				.comment(
					"Does not update fake tracks when additional chunk loading is required. This would reduce lag for" +
						" long curves, but some fake tracks would be able to placed."
				)
				.define("common.fake_tracks.disable_for_unloaded_chunk", false)
			
			val optimizeFakeTracks: BooleanValue = builder
				.comment("Enables optimized fake tracks caching. Does not dramatically improve performance. Required for middleTrack.")
				.define("common.fake_tracks.optimize", true)
			
			val noFakeTracks: BooleanValue = builder
				.comment(
					"Disables placing fake tracks entirely, except for middle tracks if configured. Fake tracks are " +
						"placed every 5 seconds (100 ticks), and placing them requires all chunks spanning curve to " +
						"be loaded. This becomes enormous burden for long curves. railx already optimizes fake track " +
						"placement, but this make it better. If good integration for your map mod exists then you " +
						"will not need it in most cases."
				)
				.define("common.fake_tracks.no_fake_tracks", false)
		}
		
		
		inner class CarriageMetadata {
			val enabled: BooleanValue = builder
				.comment("Allows carriage to have custom metadata.")
				.define("carriage_metadata.enabled", true)
			
			val entryMaxSize: IntValue = builder
				.comment("Max size of each metadata entry in bytes.")
				.defineInRange("carriage_metadata.entry_max_size", 512, 0, Int.MAX_VALUE)
			
			val carriageMaxSize: IntValue = builder
				.comment("Max size of all metadata stored for carriage, in bytes.")
				.defineInRange("carriage_metadata.entry_max_size", 512, 0, Int.MAX_VALUE)
		}
		
		
		inner class RealisticSpeed {
			val enabled: BooleanValue = builder
				.comment("Enables trains to have realistic speed")
				.define("realistic_speed.enabled", false)
			
			val removePrevious: BooleanValue = builder
				.comment("Removes previously saved realistic speed-related parameters/caches from train data.")
				.define("realistic_speed.remove_previous", false)
			
			val preciseMass: BooleanValue = builder
				.comment(
					"Calculates carriage mass based on its shape and material. If you want to specify mass for your" +
						"specific blocks, create datapack at railx.realistic_speed/materials, or implement" +
						"BlockMaterialResolver and adding into AllCustoms.Registry."
				)
				.define("realistic_speed.precise_mass", true)
			
			val power: DoubleValue = builder
				.comment("Amount of force power cars exert in kW. This replaces acceleration property of Create.")
				.defineInRange("realistic_speed.power", 800.0, 0.0, Double.POSITIVE_INFINITY)
			
			val updateTickRate: IntValue = builder
				.comment("How often physical values, such as mass, gravitational force, etc. are calculated")
				.defineInRange("realistic_speed.update_tick_rate", 3, 1, Int.MAX_VALUE)
			
			// Note: should not be less than 1.0, as navigation fails to approach. see handleApproachTargetSpeed
			val brakeAcceleration: DoubleValue = builder
				.comment("Define acceleration multiplier of brake. Proportional to Create train acceleration.")
				.defineInRange("realistic_speed.brake_acceleration", 1.5, 0.0, 100.0)
			
			val automaticBrakeAtStation: BooleanValue = builder
				.comment("Applies brake automatically if train is at train station.")
				.define("realistic_speed.brake_at_station", true)
			
			val rollingResistanceMultiplier: DoubleValue = builder
				.comment(
					"Rolling resistance multiplier; how much trains are decelerated naturally by friction between" +
						" bogey, axles and rail. Does not affect starting resistance."
				)
				.defineInRange("realistic_speed.passive_deceleration", 1.0, 0.0, Double.POSITIVE_INFINITY)
			
			val startingResistance: DoubleValue = builder
				.comment("Starting rolling resistance per block")
				.defineInRange("realistic_speed.starting_resistance", 0.03, 0.0, 1.0)
			
			val gradientTrainAcceleration: DoubleValue = builder
				.comment("How trains are accelerated according to gradient; works as gravitational constant")
				.defineInRange("realistic_speed.gradient_acceleration", 0.1, 0.0, 100.0)
			
			val curvatureResistance: DoubleValue = builder
				.comment("Factor for curvature resistance of train. 1 for default; 0 to disable.")
				.defineInRange("realistic_speed.curvature_resistance", 1.0, 0.0, 100.0)
			
			val airResistance: BooleanValue = builder
				.comment("Whether to enable air resistance")
				.define("realistic_speed.air_resistance_enabled", true)
			
			// TODO: support custom bogeys
			// TODO 2: support topping, hunting, etc. simulation: see https://en.wikipedia.org/wiki/Adhesion_railway
			// TODO 3: add slip sound and effect
			val slipEnabled: BooleanValue = builder
				.comment("Whether train slips where there are not sufficient adhesion(friction) between wheels and track.")
				.define("realistic_speed.slip.enabled", true)
			
			val slipCoefficient: DoubleValue = builder
				.comment("The coefficient of friction. The larger, the less it slips.")
				.defineInRange("realistic_speed.slip.coefficient", 0.5, 0.0, Double.POSITIVE_INFINITY)
			
			// val slipInWater: BooleanValue = builder
			// 	.comment("Train slips more underwater.")
			// 	.define("realistic_speed.slip.in_water", true)
			
			// val slipInFrozenBiome: BooleanValue = builder
			// 	.comment("Train slips more when inside frozen biome.")
			// 	.define("realistic_speed.slip.in_frozen_biome", false)
		}
		
		inner class Throttle {
			val enabled: BooleanValue = builder
				.comment("Enables throttle control, which is similar to real-life master controller.")
				.define("throttle.enabled", false)
		}
		
		inner class MiddleTrack {
			val enabled: BooleanValue = builder
				.comment("Whether to enable rendering tracks for out-of-chunk end node.")
				.define("middle_track.enabled", true)
			
			val enablePlacing: BooleanValue = builder
				.comment(
					"Whether to enable placing fake middle track. All curves, only with this option" +
						"enabled, are rendered even with out-of-chunk end node."
				)
				.define("middle_track.enable_placing", false)
			
			val removePrevious: BooleanValue = builder
				.comment("Removes all previous middle tracks.")
				.define("middle_track.remove_previous", false)
			
			val placeGap: IntValue = builder
				.comment("The gap, in chunk, at which fake middle track is placed. If larger than render distance, does not take effect.")
				.defineInRange("middle_track.place_gap", 4, 1, 16)
			
			val enableInteraction: BooleanValue = builder
				.comment(
					"Whether to show outline, relocate train, etc. for curves with out-of-chunk end node. " +
						"Some features like breaking curve are still restricted."
				)
				.define("middle_track.enable_interaction", true)
		}
		
		inner class FlexiTrak {
			val enabled: BooleanValue = builder
				.comment("You can create train track block with any angle you want.")
				.define("flexi_trak.enabled", true)
			
			val placementLength: IntValue = builder
				.comment("How long track can placed.")
				.defineInRange("flexi_trak.placement_length", 128, 1, 2048)
			
			val minRadius: IntValue = builder
				.comment("Minimum radius at which flexi tracks can be placed.")
				.defineInRange("flexi_trak.min_radius", 32, 5, 1000)
			
			val maxGradient: DoubleValue = builder
				.comment("Maximum gradient of flexi tracks. Defined as per mille (‰); 1 means ascending 1 meter while travelling 1000 meter.")
				.defineInRange("flexi_trak.max_gradient", 80.0, 0.0, 10000.0)
		}
		
		inner class BuildTrak {
			val enabled = builder
				.comment("Whether to enable tools for building tracks. This also enables placing tracks longer than maximum track length limit.")
				.define("build_trak.enabled", true)
			
			val maxPlacementLength: IntValue = builder
				.comment("Maximum placement length for each curve.")
				.defineInRange("build_trak.max_placement_length", 1024, 0, 25565)
		}
		
		val spec: ForgeConfigSpec = builder.build()
		
		companion object Value : Server(Builder())
	}
}
