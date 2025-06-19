package com.lhwdev.minecraft.railx

import net.minecraft.network.chat.Component
import net.neoforged.neoforge.common.ModConfigSpec.*
import net.neoforged.neoforge.common.TranslatableEnum


object RailXConfig {
	sealed class Server(private val builder: Builder) {
		val realisticSpeed = RealisticSpeed()
		
		val flexiTrak = FlexiTrak()
		
		inner class RealisticSpeed {
			val enabled: BooleanValue = builder
				.comment("Enables trains to have realistic speed")
				.define("realistic_speed.enabled", true)
			
			val brakeAcceleration: DoubleValue = builder
				.comment("Define acceleration of brake; braking is disabled if set to 0.")
				.defineInRange("realistic_speed.brake_acceleration", 1.3, 0.0, 100.0)
			
			val rollingResistance: DoubleValue = builder
				.comment("Rolling resistance factor; how much trains are decelerated when given no other force")
				.defineInRange("realistic_speed.passive_deceleration", 0.01, 0.0, 1.0)
			
			val startingResistance: DoubleValue = builder
				.comment("Starting rolling resistance per block")
				.defineInRange("realistic_speed.starting_resistance", 0.007, 0.0, 1.0)
			
			val gradientTrainAcceleration: DoubleValue = builder
				.comment("How trains are accelerated according to gradient; works as gravitational constant")
				.defineInRange("realistic_speed.gradient.acceleration", 0.1, 0.0, 100.0)
			
			val airResistance: EnumValue<AirResistanceLogic> = builder
				.comment("Whether to enable air resistance")
				.defineEnum("realistic_speed.air_resistance.logic", AirResistanceLogic.Off)
			
			val airResistanceMultiplier: DoubleValue = builder
				.comment("Multiplier for air resistance")
				.defineInRange("realistic_speed.air_resistance.multiplier", 1.0, 0.0, 10.0)
		}
		
		inner class FlexiTrak {
			val enabled: BooleanValue = builder
				.comment("You can create train track block with any angle you want.")
				.define("flexi_trak.enabled", true)
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
