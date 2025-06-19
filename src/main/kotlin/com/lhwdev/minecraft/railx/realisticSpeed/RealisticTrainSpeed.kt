package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.trains.entity.Carriage
import com.simibubi.create.content.trains.entity.CarriageBogey
import com.simibubi.create.content.trains.entity.CarriageContraption
import com.simibubi.create.content.trains.entity.Train
import net.minecraft.core.Direction
import kotlin.math.sqrt


class RealisticTrainSpeed(private val train: Train) {
	var brake: Double = 0.0 // from 0 to 1
	
	private val config = RailXConfig.Server.realisticSpeed
	
	var start: Long = 0
	var count = 0
	
	fun handleTickSpeed(): Boolean {
		if(!config.enabled.get()) return false
		
		if(train.manualTick || train.navigation.destination != null) {
			// TODO: do nothing
		} else {
			if(start == 0L) start = System.currentTimeMillis()
			if(count % 10 == 0) println("took ${(System.currentTimeMillis() - start) / count.toFloat()} ms for tick")
			count++
			
			train.speed = Calculator().calculateSpeed(train.speed)
		}
		train.manualTick = false
		return true
	}
	
	private inner class Calculator {
		var netAcceleration = 0.0
		var netSlowdown = 0.0
		
		private val gravitationalAcceleration: Double
		private val normalMass: Double
		private val mass: Double
		
		init {
			fun calculateBogeyGradient(bogey: CarriageBogey): Double {
				val graph = bogey.carriage.train.graph
				val delta = bogey.leading().getPosition(graph)
					.subtract(bogey.trailing().getPosition(graph))
				return delta.y / delta.length()
			}
			
			fun calculateGradient(carriage: Carriage): Double {
				val bogeyCount = if(carriage.isOnTwoBogeys) 2 else 1
				return carriage.bogeys.sumOf { if(it != null) calculateBogeyGradient(it) else 0.0 } / bogeyCount
			}
			
			var netTangentForce = 0.0
			var netNormalMass = 0.0
			var netMass = 0.0
			for(carriage in train.carriages) {
				val entity = carriage.anyAvailableEntity() ?: continue
				val mass = entity.contraption.blocks.size.toDouble() // stub implementation
				if(mass < 1.0) continue
				val sin = calculateGradient(carriage)
				netTangentForce += mass * -sin
				netNormalMass += mass * sqrt(1.0 - sin * sin)
				netMass += mass
			}
			if(netMass == 0.0) {
				gravitationalAcceleration = 0.0
				normalMass = netNormalMass
				mass = 1.0
			} else {
				gravitationalAcceleration = netTangentForce / netMass
				normalMass = netNormalMass
				mass = netMass
			}
			
			if(count % 20 == 0) println("GA=${gravitationalAcceleration} NM=${normalMass}")
		}
		
		fun calculateSpeed(previousSpeed: Double): Double {
			handleRollingResistance()
			handleGravitationalAcceleration()
			handleBrake()
			handleAirResistance()
			
			var speed = previousSpeed + (netAcceleration / 400)
			if(speed > 0) {
				speed = (speed - netSlowdown / 400).coerceAtLeast(0.0)
			} else {
				speed = (speed + netSlowdown / 400).coerceAtMost(0.0)
			}
			return speed
		}
		
		private fun handleRollingResistance() {
			if(train.speed == 0.0) {
				netSlowdown += config.startingResistance.get()
			} else {
				val factor = config.rollingResistance.get()
				val friction = factor * normalMass
				netSlowdown += friction / mass
			}
		}
		
		private fun handleGravitationalAcceleration() {
			val accelerationFactor = config.gradientTrainAcceleration.get()
			if(accelerationFactor == 0.0) return
			
			netAcceleration += accelerationFactor * gravitationalAcceleration
		}
		
		private fun handleBrake() {
			val factor = config.brakeAcceleration.get()
			if(factor == 0.0) return
			netSlowdown += brake * factor
		}
		
		private fun handleAirResistance() {
			val speed = train.speed
			if(speed == 0.0) return
			
			val area: Double
			when(config.airResistance.get()) {
				RailXConfig.Server.Value.AirResistanceLogic.Off -> return
				RailXConfig.Server.Value.AirResistanceLogic.Crude -> {
					val carriage = if(speed > 0) train.carriages.first() else train.carriages.last()
					val entity = carriage.anyAvailableEntity() ?: return
					val contraption = entity.contraption as CarriageContraption
					// TODO: area representation is inaccurate
					// TODO: contraption.bound is too large; IDK wtf is going on
					area = when(contraption.assemblyDirection.axis) {
						Direction.Axis.X -> contraption.bounds.let { it.ysize * it.zsize }
						Direction.Axis.Y -> throw IllegalStateException("axis == y")
						Direction.Axis.Z -> contraption.bounds.let { it.xsize * it.ysize }
					}
				}
			}
			
			// density at sea level, 15C; may change according to environment such as tunnel
			val density = 1.2250 /* kg/m^3 */
			
			// See 'Method of Measuring the Aerodynamic Drag of Trains'
			//   - link: https://doi.org/10.1299/jsme1958.8.390
			val dragCoefficient = 0.12 + 0.0075 * train.totalLength
			
			val v = train.speed
			netSlowdown += (0.5 * density * v * v * dragCoefficient * area) * config.airResistanceMultiplier.get()
		}
	}
}
