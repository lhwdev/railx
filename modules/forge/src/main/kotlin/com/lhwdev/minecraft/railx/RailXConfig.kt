package com.lhwdev.minecraft.railx

import net.minecraft.network.chat.Component
import net.neoforged.neoforge.common.ModConfigSpec.*
import net.neoforged.neoforge.common.TranslatableEnum


object RailXConfig {
	sealed class Server(private val builder: Builder) {
		val carriageMetadata = CarriageMetadata()
		
		val realisticSpeed = RealisticSpeed()
		
		val middleTrack = MiddleTrack()
		
		val flexiTrak = FlexiTrak()
		
		
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
				.define("realistic_speed.enabled", true)
			
			val updateTickRate: IntValue = builder
				.comment("How often physical values, such as mass, gravitational force, etc. are calculated")
				.defineInRange("realistic_speed.update_tick_rate", 3, 1, Int.MAX_VALUE)
			
			val brakeAcceleration: DoubleValue = builder
				.comment("Define acceleration of brake; braking is disabled if set to 0.")
				.defineInRange("realistic_speed.brake_acceleration", 1.3, 0.0, 100.0)
			
			val rollingResistance: DoubleValue = builder
				.comment("Rolling resistance factor; how much trains are decelerated when given no other force")
				.defineInRange("realistic_speed.passive_deceleration", 0.01, 0.0, 1.0)
			
			val startingResistance: DoubleValue = builder
				.comment("Starting rolling resistance per block")
				.defineInRange("realistic_speed.starting_resistance", 0.03, 0.0, 1.0)
			
			val gradientTrainAcceleration: DoubleValue = builder
				.comment("How trains are accelerated according to gradient; works as gravitational constant")
				.defineInRange("realistic_speed.gradient_acceleration", 0.1, 0.0, 100.0)
			
			val curvatureResistance: DoubleValue = builder
				.comment("Factor for curvature resistance of train. 1 for default; 0 to disable.")
				.defineInRange("realistic_speed.curvature_resistance", 1.0, 0.0, 100.0)
			
			val airResistance: EnumValue<AirResistanceLogic> = builder
				.comment("Whether to enable air resistance")
				.defineEnum("realistic_speed.air_resistance.logic", AirResistanceLogic.Off)
			
			val airResistanceMultiplier: DoubleValue = builder
				.comment("Multiplier for air resistance")
				.defineInRange("realistic_speed.air_resistance.multiplier", 1.0, 0.0, 10.0)
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
			
			val placeGap: IntValue = builder
				.comment("The gap, in chunk, at which fake middle track is placed. If larger than render distance, does not take effect.")
				.defineInRange("middle_track.place_gap", 4, 1, 16)
			
			val enableInteraction: BooleanValue = builder
				.comment(
					"Whether to show outline, relocate train, etc. for curves with out-of-chunk end node. " +
						"Some features like breaking curve are still restricted."
				)
				.define("middle_track.enable_interaction", false)
		}
		
		inner class FlexiTrak {
			val enabled: BooleanValue = builder
				.comment("You can create train track block with any angle you want.")
				.define("flexi_trak.enabled", true)
			
			// val blend: BooleanValue = builder
			// 	.comment(
			// 		"flexi tracks are treated as same as normal create train tracks. Cannot get flexi_track " +
			// 			"item, but all features of flexi track is available to normal train tracks. If disabled, new " +
			// 			"recipe for flexi_track is added."
			// 	)
			// 	.define("flexi_trak.blend", false)
			
			val minRadius: IntValue = builder
				.comment("Max radius at which flexi tracks can be placed.")
				.defineInRange("flexi_trak.min_radius", 32, 5, 1000)
		}
		
		val spec = builder.build()
		
		companion object Value : Server(Builder()) {
			enum class AirResistanceLogic(val displayName: String) : TranslatableEnum {
				Off("off"), Crude("crude");
				
				override fun getTranslatedName(): Component = Component.literal(displayName)
			}
		}
	}
}
