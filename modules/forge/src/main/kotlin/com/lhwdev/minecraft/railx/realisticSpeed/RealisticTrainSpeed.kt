package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.utils.ContraptionLevelReader
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInteractionBehaviour
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock
import com.simibubi.create.content.trains.entity.Carriage
import com.simibubi.create.content.trains.entity.CarriageBogey
import com.simibubi.create.content.trains.entity.CarriageContraption
import com.simibubi.create.content.trains.entity.Train
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import java.io.*
import kotlin.math.*


class RealisticTrainSpeed(private val train: Train) {
	var brake: Double = 0.0 // from 0 to 1
	
	private val config get() = RailXConfig.Server.realisticSpeed
	
	var p: TrainProps? = null
	var powerAmount: Double = Double.NaN
	
	
	// NOTE: handleApproachTargetSpeed is called prior to handleTickSpeed
	//       as, in create:CommonEvents.onServerWorldTick, ControlsServerHandler.tick() then Create.RAILWAYS.tick().
	fun handleApproachTargetSpeed(@Suppress("unused") accelerationMod: Float): Boolean {
		if(!config.enabled.isTrue) return false
		
		currentSpeed = train.speed
		handleTargetSpeed()
		
		// TODO: we do not use accelerationMod,
		val acceleration = train.acceleration() /* * accelerationMod */
		return if(train.navigation.destination != null) {
			approachAcceleration = 0.0
			false // run pre-mixin original code,
			// if(previousSpeed < target) train.speed = min(previousSpeed + acceleration, target)
			// else train.speed = max(previousSpeed - acceleration, target)
		} else {
			// train.leaveStation() // handled by handleTickSpeed() -> calculateSpeed()
			approachAcceleration = if(targetSpeedSource != 0.0) {
				sign(targetSpeed - currentSpeed) * acceleration * 400
			} else {
				// passive deceleration where targetSpeed = 0, acceleration = (opposite)
				0.0
			}
			true
		}
	}
	
	fun handleTickSpeed(): Boolean {
		if(!config.enabled.isTrue) return false
		if(train.derailed) return false
		
		currentSpeed = train.speed
		// if(train.navigation.destination != null) {
		// 	skipCount = 0
		// 	stoppedFor = 0
		// } else {
		val speed = calculateSpeed()
		train.speed = speed
		train.manualTick = false
		return true
	}
	
	fun handleTargetSpeed() {
		val source = train.targetSpeed
		var target = source
		if(target == targetSpeedSource) return
		
		// Behavior of manual tick: passive -> targetSpeed=0, acceleration=-1 / break
		if(train.manualTick) target = when(Mth.sign(currentSpeed * target)) {
			1 -> target
			0 -> target
			-1 -> 0.0
			else -> throw NoWhenBranchMatchedException()
		}
		targetSpeedSource = source
		targetSpeed = target
	}
	
	fun read(tag: CompoundTag) {
		val props = tag.getByteArray("Props")
		if(props.isNotEmpty()) {
			p = TrainProps.read(DataInputStream(ByteArrayInputStream(props)))
		}
		if("Power" in tag) powerAmount = tag.getDouble("Power")
	}
	
	fun write(): CompoundTag? {
		if(config.removePrevious.isTrue) return null
		val tag = CompoundTag()
		p?.let { p ->
			tag.putByteArray("Props", ByteArrayOutputStream().also { p.write(DataOutputStream(it)) }.toByteArray())
		}
		if(powerAmount.isFinite()) tag.putDouble("Power", powerAmount)
		return tag
	}
	
	
	private var skipCount = 0
	private var stoppedFor = 0
	
	private var debugEnabled = false
	private var debugCounter = 0
	private fun debug(text: String) {
		if(debugCounter == 0 && stoppedFor < 20) println("railx:realistic $text")
	}
	
	private fun debugHandle(key: String, log: String) {
		if(!debugEnabled) return
		debug("railx:rs[name=${train.name.tryCollapseToString()}] $key: $log")
	}
	
	fun calculateSpeed(): Double {
		handleTargetSpeed()
		
		val threshold = if(stoppedFor >= 20) 40 else config.updateTickRate.asInt
		val willUpdate = skipCount >= threshold || approachAcceleration != 0.0
		if(willUpdate) {
			updateSpeed()
			skipCount = 0
		}
		if(debugCounter++ >= 5) debugCounter = 0
		
		val acceleration = netWheelAcceleration + netEnvironmentalAcceleration
		val slowdown = netWheelSlowdown + netEnvironmentalSlowdown
		var speed = currentSpeed + acceleration / 400
		
		fun applySlowdown(s: Double) = if(s > 0) {
			(s - slowdown / 400).coerceAtLeast(0.0)
		} else {
			(s + slowdown / 400).coerceAtMost(0.0)
		}
		
		if(approachAcceleration != 0.0) {
			val a = applySlowdown(speed)
			val b = applySlowdown(speed + approachAcceleration / 400)
			speed = targetSpeed.coerceIn(min(a, b), max(a, b))
		} else {
			speed = applySlowdown(speed)
		}
		train.currentStation?.let { station ->
			if(abs(speed) < 0.01) return@let
			train.leaveStation()
		}
		
		if(skipCount != Int.MAX_VALUE) skipCount++
		if(speed == 0.0) {
			if(stoppedFor != Int.MAX_VALUE) stoppedFor++
		} else {
			stoppedFor = 0
		}
		
		approachAcceleration = 0.0
		return speed
	}
	
	private var approachAcceleration = 0.0
	
	private var netEnvironmentalAcceleration = 0.0
	private var netWheelAcceleration = 0.0
	private var netEnvironmentalSlowdown = 0.0
	private var netWheelSlowdown = 0.0
	private var slipAmount = 0.0
	
	private var currentSpeed: Double = 0.0
	private var targetSpeed: Double = 0.0
	private var targetSpeedSource: Double = 0.0
	private var mass: Double = 1.0
	
	private var gravitationalAcceleration: Double = 0.0
	private var normalMassRatio: Double = 0.0
	private var curveRadius: Double = 100000.0
	
	class TrainProps(
		val rollingFactor: Double,
		val rolling2Factor: Double,
		val headAirResistanceFactor: Double,
		val tailAirResistanceFactor: Double,
		
		val carriageCount: Int,
		val carriageMass: IntArray,
	) {
		fun write(output: DataOutput) {
			output.writeByte(0)
			output.writeByte(carriageCount)
			output.writeDouble(rollingFactor)
			output.writeDouble(rolling2Factor)
			output.writeDouble(headAirResistanceFactor)
			output.writeDouble(tailAirResistanceFactor)
			for(mass in carriageMass) output.writeInt(mass)
		}
		
		companion object {
			fun read(input: DataInput): TrainProps = when(val v = input.readUnsignedByte()) {
				0 -> {
					val carriageCount = input.readUnsignedByte()
					TrainProps(
						rollingFactor = input.readDouble(),
						rolling2Factor = input.readDouble(),
						headAirResistanceFactor = input.readDouble(),
						tailAirResistanceFactor = input.readDouble(),
						
						carriageCount = carriageCount,
						carriageMass = IntArray(carriageCount) { input.readInt() },
					)
				}
				
				else -> throw IllegalStateException("unknown version $v")
			}
		}
	}
	
	
	fun updateSpeed() {
		netEnvironmentalAcceleration = 0.0
		netWheelAcceleration = 0.0
		netEnvironmentalSlowdown = 0.0
		netWheelSlowdown = 0.0
		
		if(train.presentDimensions.isEmpty()) return
		
		ensureTrainProps()
		updatePhysicalState()
		
		handleRollingResistance()
		handleGravitationalAcceleration()
		handleBrake()
		handleCurvatureResistance()
		handleAirResistance()
		
		handleSlip()
		
		// if(debugEnabled && debugCounter == 0 && stoppedFor <= 20)
		// 	for((key, value) in debugEntries) println("$key = $value")
		// debugEntries.keys.forEach { debugEntries[it] = "" }
	}
	
	// private val debugEntries = mutableMapOf<String, String>()
	private fun onResult(key: String, value: Double, debug: String = ""): Double {
		// debugEntries[key] = "$value $debug"
		return value
	}
	
	/**
	 * The following is created by Armstrong and Swift, and exact formular is referenced from
	 * https://doi.org/10.1243/0954409001531306. Note that, in the formula below, every mass is expressed in ton (t).
	 *
	 * ```latex
	 * A = 6.4 * trailerCarMass + 8.0 * powerCarMass
	 * B = 0.18 * mass + 1 * trailerCars + 0.005 * powerCars * totalPower
	 * C = (0.6125 * endDragCoefficient * crossSection)
	 *     + 0.00197 * perimeter * length
	 *     + 0.0021 * perimeter * carriageGap * (trailerCars + powerCars - 1)
	 *     + 0.2061 * bogieDragCoefficient * bogies
	 *     + 0.2566 * pantographCount
	 * ```
	 */
	fun ensureTrainProps(): TrainProps? {
		p?.let { return it }
		val carriages = train.carriages
		if(!carriages.all { it.anyAvailableEntity() != null }) return null
		
		val preciseMass = config.preciseMass.isTrue
		
		var netMass = 0
		val carriageMass = IntArray(carriages.size)
		var powerCarMass = 0
		var trailerCarMass = 0
		var powerCars = 0
		var trailerCars = 0
		
		var crossSection = 0.0
		var length = 0
		var carriageGapSum = 0.0
		var bogieDrags = 0.0
		
		var previousTrailingLength = 0
		for((index, carriage) in carriages.withIndex()) {
			val entity = carriage.anyAvailableEntity()!!
			val contraption = entity.contraption as CarriageContraption // TODO: does this work for train in portal? ...
			val blocks = contraption.blocks
			if(blocks.isEmpty()) continue
			
			val contraptionLevel = ContraptionLevelReader(entity.level(), contraption)
			if(preciseMass) contraptionLevel.blockEntities.load()
			
			val mass = if(preciseMass) {
				val cache = Object2DoubleOpenHashMap<Any>()
				blocks.values.sumOf {
					cache.getOrPut(BlockMaterials.cacheKey(contraptionLevel, it.pos, it.state)) {
						BlockMaterials.resolve(contraptionLevel, it.pos, it.state).mass
					}
				}.toInt()
			} else blocks.size
			carriageMass[index] = mass
			netMass += mass
			
			val isPowerCar = contraption.interactors.values.any { it is ControlsInteractionBehaviour }
			if(isPowerCar) {
				powerCars++
				powerCarMass += mass
			} else {
				trailerCars++
				trailerCarMass += mass
			}
			
			
			// NOTE: There is article that, if you don't know bogie drag coefficient, just exclude that term. Should I?
			// TODO: make it differ by its type; use CFD to compute existing bogies (including Steam 'n Rails)
			val bogieDragCoefficient = 0.2
			val bogieCount = if(carriage.isOnTwoBogeys) 2 else 1
			bogieDrags += bogieDragCoefficient * bogieCount
			
			// TODO:
			var carriageCrossArea = 0.0
			var carriageCrossCount = 0
			val direction = contraption.assemblyDirection
			val axis = direction.axis
			val bounds = AABBi(blocks.keys.first())
			for(blockPos in blocks.keys) bounds.growToInclude(blockPos)
			
			val mutablePos = BlockPos.MutableBlockPos()
			val bogiePos = mutableListOf<Int>()
			bounds.forBlockAxis(axis) { a ->
				var planeCrossArea = 0
				bounds.forBlockPlane(axis) plane@{ b, c ->
					mutablePos.setCycled(axis, a, b, c)
					val block = blocks[mutablePos] ?: return@plane
					
					planeCrossArea += block.state.getCollisionShape(contraptionLevel, mutablePos).calculateVolume()
					if(block.state.block is AbstractBogeyBlock<*>) {
						// TODO: compat for train in train mod
						bogiePos += a
					}
				}
				if(planeCrossArea >= BlockVolume * 3) {
					carriageCrossArea += planeCrossArea / BlockVolume
					carriageCrossCount++
				}
			}
			if(carriageCrossCount != 0) crossSection += carriageCrossArea / carriageCrossCount / carriages.size
			
			length += bounds.max(axis) - bounds.min(axis)
			if(index != 0) {
				val leadingLength = bogiePos.first() - bounds.min(axis)
				check(leadingLength >= 0) { RailX.errorBreakpoint() }
				val gap = train.carriageSpacing[index - 1] - previousTrailingLength - leadingLength
				length += gap
				carriageGapSum += gap.coerceAtLeast(1)
			}
			previousTrailingLength = bounds.max(axis) - bogiePos.last()
		}
		
		val totalPower = if(powerAmount.isNaN()) config.power.asDouble else powerAmount
		val perimeter = sqrt(crossSection / PI)
		val carriageGap = carriageGapSum / carriages.size
		
		val airResistanceFactor = 0.6125 * 0.4 /* DUMMY endDragCoefficient */ * crossSection +
			0.00197 * perimeter * length +
			0.0021 * perimeter * carriageGap * (trailerCars + powerCars - 1) +
			0.2061 * bogieDrags
		// plus 0.2566 * pantographCount // TODO: compat for mods like Create: Tramways to determine this
		
		return TrainProps(
			rollingFactor = 0.0064 * trailerCarMass + 0.008 * powerCarMass,
			rolling2Factor = 0.00018 * netMass + trailerCars + 0.005 * powerCars * totalPower,
			headAirResistanceFactor = airResistanceFactor,
			tailAirResistanceFactor = airResistanceFactor,
			
			carriageCount = carriages.size,
			carriageMass = carriageMass,
		).also { p = it }
	}
	
	private fun updatePhysicalState() {
		val p = p ?: return
		mass = p.carriageMass.sumOf { it.toDouble() }
		
		if(train.carriages.any {
				it.leadingBogey().leading().edge == null ||
					it.leadingBogey().trailing().edge == null ||
					it.trailingBogey().leading().edge == null ||
					it.trailingBogey().trailing().edge == null
			}) return
		
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
		
		fun calculateHorizontalCurvature(carriage: Carriage): Double {
			val leading = carriage.leadingPoint
			val trailing = carriage.trailingPoint
			val leadingDirection = leading.edge.getDirectionAt(leading.position)
			val trailingDirection = trailing.edge.getDirectionAt(trailing.position)
			// b/2 = r cos(90-T/2) = r sqrt((1 - cos T)/2), r = b / [2 sqrt((1 - cos T)/2)]
			// cos T = leadingDirection dot trailingDirection
			val offset = trailing.getPosition(train.graph) - leading.getPosition(train.graph)
			val dot = leadingDirection.x * trailingDirection.x + leadingDirection.z * trailingDirection.z
			val size = sqrt(leadingDirection.horizontalDistanceSqr() * trailingDirection.horizontalDistanceSqr())
			// adding 1e-8 because if dot == size (mathematically), sometimes size > dot (computer)
			return (2 * sqrt((1 + 1e-10 - dot / size) / 2)) / offset.length()
		}
		
		var netTangentForce = 0.0
		var netNormalMass = 0.0
		var netCurvature = 0.0
		for((index, carriage) in train.carriages.withIndex()) {
			val mass = p.carriageMass[index]
			if(mass < 1) continue
			val sin = calculateGradient(carriage)
			
			netTangentForce += mass * -sin
			netNormalMass += mass * sqrt(1.0 - sin * sin)
			netCurvature += calculateHorizontalCurvature(carriage)
		}
		
		if(mass < 0.1e-3) {
			gravitationalAcceleration = 0.0
			normalMassRatio = 0.0
			mass = 1.0
		} else {
			gravitationalAcceleration = netTangentForce / mass
			normalMassRatio = netNormalMass / mass
		}
		if(netCurvature similarTo 0.0) {
			curveRadius = Double.POSITIVE_INFINITY
		} else {
			curveRadius = (1 / netCurvature) / train.carriages.size
		}
	}
	
	private fun handleRollingResistance() {
		val startingResistance = config.startingResistance.asDouble
		if(currentSpeed == 0.0) {
			netWheelSlowdown += onResult("rollingResistance", startingResistance)
		} else {
			val p = p ?: return
			var resistance = (p.rollingFactor + p.rolling2Factor * abs(currentSpeed)) / mass * normalMassRatio
			val creepingThreshold = 0.03
			val creeping = creepingThreshold - abs(currentSpeed)
			if(creeping > 0) {
				val creepingSlowdown = creeping * startingResistance / creepingThreshold
				val creepingResult = creepingSlowdown + 0.4 * ((creepingSlowdown + 1).pow2() - 1)
				resistance += creepingResult
			}
			
			netWheelSlowdown += onResult("rollingResistance", resistance)
		}
	}
	
	private fun handleGravitationalAcceleration() {
		val accelerationFactor = config.gradientTrainAcceleration.asDouble
		if(accelerationFactor == 0.0) return
		
		val result = accelerationFactor * gravitationalAcceleration
		netEnvironmentalAcceleration += onResult("gravitationalAcceleration", result)
	}
	
	private fun handleBrake() {
		val factor = config.brakeAcceleration.asDouble
		if(factor == 0.0) return
		
		var brake = this.brake
		if(config.automaticBrakeAtStation.isTrue && train.currentStation != null && approachAcceleration == 0.0) {
			brake = max(brake, 1.0)
		}
		netWheelSlowdown += brake * factor
	}
	
	private object CurvatureResistance {
		// r = c / (r-a); r' = -c / (r-a)^2
		// R = 650/(r-55), L = c/r + k; L(p) = R(p), L'(p) = R'(p)
		// c/p + k = 650/(p-55), c/p^2 = 650/(p-55)^2
		// c = 650 p^2 / (p-55)^2, k = 650/(p-55) - c/p
		const val criticalPoint = 155
		const val minimumPoint = 20
		const val multiplier = 650
		const val c = multiplier * (criticalPoint * criticalPoint) / ((criticalPoint - 55) * (criticalPoint - 55))
		const val k = multiplier / (criticalPoint - 55) - c / criticalPoint
		
		fun calculate(radius: Double) = when {
			radius > criticalPoint -> multiplier / (radius - 55)
			radius > minimumPoint -> c / radius + k
			else -> c / minimumPoint.toDouble() + k
		}
	}
	
	private fun handleCurvatureResistance() {
		val factor = config.curvatureResistance.asDouble
		if(factor == 0.0) return
		
		val resistance = CurvatureResistance.calculate(curveRadius)
		val result = factor * resistance * (sqrt(abs(train.speed) + 4) - 2)
		netEnvironmentalSlowdown += onResult("curvatureResistance", result, debug = "R=$curveRadius")
	}
	
	private fun handleAirResistance() {
		val speed = train.speed
		if(speed == 0.0) return
		
		// TODO: tunnel
		val p = p ?: return
		if(!config.airResistance.isTrue) return
		
		// TODO: is speed > 0 means carriage[0] is taking forward? I don't think so
		val factor = if(speed > 0) p.headAirResistanceFactor else p.tailAirResistanceFactor
		if(factor == 0.0) { // config.airResistance changed OFF -> ON
			this.p = null
			ensureTrainProps()
			return
		}
		val result = (factor * speed * speed) / mass
		netEnvironmentalSlowdown += onResult("airResistance", result)
	}
	
	private fun handleSlip() {
		// slips are not generally happen, but they might happen where gradient is too large
		if(!config.slipEnabled.isTrue) {
			slipAmount = 0.0
			return
		}
		
		val friction = config.slipCoefficient.asDouble * normalMassRatio
		// TODO: slip and underwater / frozen biome / ...etc
		val wheelAcceleration = approachAcceleration + netWheelAcceleration - sign(train.speed) * netWheelSlowdown
		val result = abs(wheelAcceleration) - friction
		if(result > 0) {
			slipAmount = result
			netEnvironmentalAcceleration += -sign(wheelAcceleration) * result
		} else {
			slipAmount = 0.0
		}
	}
}


private class AABBi(
	var minX: Int, var minY: Int, var minZ: Int,
	var maxX: Int, var maxY: Int, var maxZ: Int,
) {
	constructor(pos: Vec3i) : this(
		minX = pos.x, minY = pos.y, minZ = pos.z,
		maxX = pos.x + 1, maxY = pos.y + 1, maxZ = pos.z + 1,
	)
	
	fun growToInclude(pos: Vec3i) {
		val x = pos.x
		val y = pos.y
		val z = pos.z
		
		if(minX > x) minX = x
		else if(maxX <= x) maxX = x + 1
		
		if(minY > y) minY = y
		else if(maxY <= y) maxY = y + 1
		
		if(minZ > z) minZ = z
		else if(maxZ <= z) maxZ = z + 1
	}
	
	fun min(axis: Direction.Axis) = when(axis) {
		Direction.Axis.X -> minX
		Direction.Axis.Y -> minY
		Direction.Axis.Z -> minZ
	}
	
	fun max(axis: Direction.Axis) = when(axis) {
		Direction.Axis.X -> maxX
		Direction.Axis.Y -> maxY
		Direction.Axis.Z -> maxZ
	}
}

private operator fun AABBi.get(direction: Direction): Int =
	if(direction.axisDirection == Direction.AxisDirection.POSITIVE) max(direction.axis) else min(direction.axis)

private inline fun AABBi.forBlockAxis(axis: Direction.Axis, block: (Int) -> Unit) {
	for(component in min(axis)..<max(axis)) {
		block(component)
	}
}

private val Direction.Axis.nextAxis
	get() = when(this) {
		Direction.Axis.X -> Direction.Axis.Y
		Direction.Axis.Y -> Direction.Axis.Z
		Direction.Axis.Z -> Direction.Axis.X
	}

private inline fun AABBi.forBlockPlane(normal: Direction.Axis, block: (a: Int, b: Int) -> Unit) {
	val u = normal.nextAxis
	val v = u.nextAxis
	forBlockAxis(u) { a ->
		forBlockAxis(v) { b ->
			block(a, b)
		}
	}
}

private fun BlockPos.MutableBlockPos.setCycled(axis: Direction.Axis, a: Int, b: Int, c: Int) {
	when(axis) {
		Direction.Axis.X -> set(a, b, c)
		Direction.Axis.Y -> set(c, a, b)
		Direction.Axis.Z -> set(b, c, a)
	}
}
