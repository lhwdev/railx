package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.utils.pow2
import com.simibubi.create.content.trains.entity.Carriage
import com.simibubi.create.content.trains.entity.CarriageBogey
import com.simibubi.create.content.trains.entity.CarriageContraption
import com.simibubi.create.content.trains.entity.Train
import net.minecraft.core.Direction
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import kotlin.math.abs
import kotlin.math.sqrt


class RealisticTrainSpeed(private val train: Train) {
	var brake: Double = 0.0 // from 0 to 1
	
	private val config = RailXConfig.Server.realisticSpeed
	
	fun handleTickSpeed(): Boolean {
		if(!config.enabled.get()) return false
		
		if(train.manualTick || train.navigation.destination != null) {
			// TODO: do nothing
			skipCount = 0
			stoppedFor = 0
		} else {
			val speed = calculateSpeed(train.speed)
			train.speed = speed
		}
		train.manualTick = false
		return true
	}
	
	
	private var skipCount = 0
	private var stoppedFor = 0
	
	private var debugCounter = 0
	private fun debug(text: String) {
		if(debugCounter == 0 && stoppedFor < 20) println("railx:realistic $text")
	}
	
	fun calculateSpeed(previousSpeed: Double): Double {
		val threshold = if(stoppedFor >= 20) 40 else config.updateTickRate.get()
		if(skipCount >= threshold) {
			updateSpeed()
			skipCount = 0
		}
		if(debugCounter++ >= 20) debugCounter = 0
		
		var speed = previousSpeed + (netAcceleration / 400)
		speed = if(speed > 0) {
			(speed - netSlowdown / 400).coerceAtLeast(0.0)
		} else {
			(speed + netSlowdown / 400).coerceAtMost(0.0)
		}
		
		if(skipCount != Int.MAX_VALUE) skipCount++
		if(speed == 0.0) {
			if(stoppedFor != Int.MAX_VALUE) stoppedFor++
		} else {
			stoppedFor = 0
		}
		
		return speed
	}
	
	private var netAcceleration = 0.0
	private var netSlowdown = 0.0
	
	private var gravitationalAcceleration: Double = 0.0
	private var normalMass: Double = 0.0
	private var mass: Double = 1.0
	private var curveRadius: Double = 100000.0
	
	fun updateSpeed() {
		netAcceleration = 0.0
		netSlowdown = 0.0
		
		updatePhysicalState()
		
		handleRollingResistance()
		handleGravitationalAcceleration()
		handleBrake()
		handleCurvatureResistance()
		handleAirResistance()
	}
	
	private fun updatePhysicalState() {
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
		
		fun calculateRadius(carriage: Carriage): Double {
			val leading = carriage.leadingPoint
			val trailing = carriage.trailingPoint
			val leadingDirection = leading.edge.getDirectionAt(leading.position)
			val trailingDirection = trailing.edge.getDirectionAt(trailing.position)
			// b/2 = r cos(90-T/2) = r sqrt((1 - cos T)/2), r = b / [2 sqrt((1 - cos T)/2)]
			// cos T = leadingDirection dot trailingDirection
			val offset = trailing.getPosition(train.graph) - leading.getPosition(train.graph)
			return offset.length() / (2 * sqrt((1 - leadingDirection.dot(trailingDirection)) / 2))
		}
		
		var netTangentForce = 0.0
		var netNormalMass = 0.0
		var netMass = 0.0
		var netRadius = 0.0
		for(carriage in train.carriages) {
			val entity = carriage.anyAvailableEntity() ?: continue
			val mass = entity.contraption.blocks.size.toDouble() // stub implementation
			if(mass < 1.0) continue
			
			val sin = calculateGradient(carriage)
			netTangentForce += mass * -sin
			netNormalMass += mass * sqrt(1.0 - sin * sin)
			netMass += mass
			netRadius += calculateRadius(carriage)
		}
		
		if(abs(netMass) < 0.1e-3) {
			gravitationalAcceleration = 0.0
			normalMass = netNormalMass
			mass = 1.0
		} else {
			gravitationalAcceleration = netTangentForce / netMass
			normalMass = netNormalMass
			mass = netMass
		}
		curveRadius = netRadius / train.carriages.size
		debug("curveRadius=$curveRadius")
	}
	
	private fun handleRollingResistance() {
		val startingResistance = config.startingResistance.get()
		if(train.speed == 0.0) {
			netSlowdown += config.startingResistance.get()
		} else {
			// if train is very slow, urge it to stop in around 1s
			val stopThreshold = startingResistance / 20
			val speed = abs(train.speed)
			if(speed < stopThreshold) {
				netSlowdown += config.startingResistance.get() * (stopThreshold - speed) / stopThreshold
			}
			
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
	
	private object CurvatureResistance {
		// r = c / (r-a); r' = -c / (r-a)^2
		// R = 650/(r-55), L = c/r + k; L(p) = R(p), L'(p) = R'(p)
		// c/p + k = 650/(p-55), c/p^2 = 650/(p-55)^2
		// c = 650 p^2 / (p-55)^2, k = 650/(p-55) - c/p
		const val criticalPoint = 155
		const val c = 650 * (criticalPoint * criticalPoint) / ((criticalPoint - 55) * (criticalPoint - 55))
		const val k = 650 / (criticalPoint - 55) - c / criticalPoint
		
		fun calculate(radius: Double) = if(radius > criticalPoint) {
			650 / (radius - 55)
		} else {
			c / radius + k
		}
	}
	
	private fun handleCurvatureResistance() {
		val factor = config.curvatureResistance.get()
		if(factor == 0.0) return
		
		val resistance = CurvatureResistance.calculate(curveRadius)
		netSlowdown += resistance
	}
	
	private fun handleAirResistance() {
		val speed = train.speed
		if(speed == 0.0) return
		
		// TODO: take my tunnel factor
		
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
