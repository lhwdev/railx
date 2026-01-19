package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.throttle.Throttles
import com.lhwdev.minecraft.railx.utils.ContraptionLevelReader
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.minus
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
import java.io.*
import kotlin.math.*


interface ITrainWithRealisticTrainSpeed {
	val `railx$realisticSpeed`: RealisticTrainSpeed?
}

val Train.realisticSpeed: RealisticTrainSpeed?
	get() = (this as ITrainWithRealisticTrainSpeed).`railx$realisticSpeed`


class RealisticTrainSpeed(private val train: Train) {
	private val config get() = RailXConfig.Server.realisticSpeed
	
	var p: TrainProps? = null
	var powerAmount: Double = Double.NaN
	
	
	private val manualControl: Boolean
		get() = train.manualTick || train.carriages.any { carriage ->
			carriage.entities.values.any { it.entity.get()?.controllingPlayer != null }
		}
	
	private var currentSpeed: Double = 0.0
	private var targetSpeed: Double = 0.0
	private var targetThrottle: Double = 0.0
	private var targetBreak: Double = 0.0
	private var targetSpeedHandled = false
	private var targetUpdated = false
	
	// NOTE: handleApproachTargetSpeed is called prior to handleTickSpeed
	//       as, in create:CommonEvents.onServerWorldTick, ControlsServerHandler.tick() then Create.RAILWAYS.tick().
	fun handleApproachTargetSpeed(@Suppress("unused") accelerationMod: Float): Boolean {
		if(!config.enabled.get()) return false
		
		currentSpeed = train.speed
		
		onHandleTargetSpeed {
			handleTargetSpeed(target = train.targetSpeed)
			
			// In RailXConfig.Server.realisticSpeed.brakeAcceleration, it ensures targetBreak >= 1. (if not zero)
			// TODO: need to adjust break to less than 1.5 or something, to be exactly 1.0?
			
			targetThrottle *= accelerationMod
			targetBreak *= accelerationMod
		}
		
		return true
	}
	
	fun handleManualThrottle(throttle: Throttles.Throttle, target: Double): Unit = onHandleTargetSpeed {
		targetSpeedHandled = true
		
		val force = 400 * train.acceleration().toDouble()
		val gear = throttle.acceleration
		when {
			gear == 0.0 -> {
				targetSpeed = currentSpeed
				targetThrottle = 0.0
				targetBreak = 0.0
			}
			
			gear > 0.0 -> {
				targetSpeed = target
				targetThrottle = force * gear * sign(target)
				targetBreak = 0.0
			}
			
			else -> {
				targetSpeed = 0.0
				targetThrottle = 0.0
				targetBreak = force * -gear
			}
		}
	}
	
	// TODO: - need more acceleration on start(ie. speed <= 0.01)?
	//       - apply natural slowdown when approaching close to top speed
	private fun handleTargetSpeed(target: Double) {
		targetSpeedHandled = true
		// in manual control, target = [throttle -> maxSpeed * direction, neutral -> 0, break -> -maxSpeed * direction]
		val force = 400 * train.acceleration().toDouble()
		if(manualControl && train.navigation.destination == null) {
			val direction = Mth.sign(currentSpeed * target)
			when {
				direction == 1 || currentSpeed == 0.0 -> { // throttle
					targetSpeed = target
					targetThrottle = force * sign(target)
					targetBreak = 0.0
				}
				
				direction == 0 -> { // neutral
					targetSpeed = currentSpeed
					targetThrottle = 0.0
					targetBreak = 0.0
				}
				
				direction == -1 -> { // break
					targetSpeed = 0.0
					targetThrottle = 0.0
					targetBreak = force
				}
				
				else -> throw NoWhenBranchMatchedException()
			}
		} else {
			targetSpeed = target
			val direction = sign(target - currentSpeed)
			if(direction == 0.0) {
				targetThrottle = 0.0
				targetBreak = 0.0
			} else {
				if(currentSpeed * direction >= 0) { // throttle
					targetThrottle = force * direction
					targetBreak = 0.0
				} else { // break
					targetThrottle = 0.0
					targetBreak = force
				}
			}
		}
	}
	
	private fun handleDefaultTargetSpeed() {
		targetSpeedHandled = true
		
		val isIdle = !manualControl && train.navigation.destination == null
		if(isIdle) {
			if(targetThrottle != 0.0 || targetBreak != 0.0) targetUpdated = true
			targetSpeed = 0.0
			targetThrottle = 0.0
			targetBreak = 0.0
		}
	}
	
	private inline fun <R> onHandleTargetSpeed(block: () -> R): R {
		val previousSpeed = targetSpeed
		val previousThrottle = targetThrottle
		val previousBreak = targetBreak
		return try {
			block()
		} finally {
			if(previousSpeed != targetSpeed || previousThrottle != targetThrottle || previousBreak != targetBreak)
				targetUpdated = true
		}
	}
	
	
	fun handleTickSpeed(): Boolean {
		if(!config.enabled.get()) return false
		if(train.derailed) return false
		
		currentSpeed = train.speed
		if(!targetSpeedHandled)
			handleDefaultTargetSpeed()
		
		val speed = calculateSpeed()
		
		train.speed = speed
		train.manualTick = false
		targetSpeedHandled = false
		targetUpdated = false
		return true
	}
	
	
	fun read(tag: CompoundTag) {
		val props = tag.getByteArray("Props")
		if(props.isNotEmpty()) {
			p = TrainProps.read(DataInputStream(ByteArrayInputStream(props)))
		}
		if("Power" in tag) powerAmount = tag.getDouble("Power")
	}
	
	fun write(): CompoundTag? {
		if(config.removePrevious.get()) return null
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
	private val debugEntries = mutableMapOf<String, String>()
	
	fun calculateSpeed(): Double {
		val threshold = if(stoppedFor < 20 || targetSpeed != 0.0) config.updateTickRate.get() else 40
		val willUpdate = skipCount >= threshold || targetUpdated
		if(willUpdate) {
			updateSpeed()
			skipCount = 0
		}
		
		val acceleration = netWheelAcceleration + netEnvironmentalAcceleration
		val slowdown = netWheelSlowdown + netEnvironmentalSlowdown
		var speed = currentSpeed + acceleration / 400
		
		fun applySlowdown(s: Double) = if(s > 0) {
			(s - slowdown / 400).coerceAtLeast(0.0)
		} else {
			(s + slowdown / 400).coerceAtMost(0.0)
		}
		
		if(targetThrottle != 0.0) {
			// in case targetThrottle overshoots
			val a = applySlowdown(speed)
			val b = applySlowdown(speed + targetThrottle / 400)
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
		
		return speed
	}
	
	private var netEnvironmentalAcceleration = 0.0
	private var netWheelAcceleration = 0.0
	private var netEnvironmentalSlowdown = 0.0
	private var netWheelSlowdown = 0.0
	
	var slipAmount = 0.0
		private set
	
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
		debugEntries.clear()
		netEnvironmentalAcceleration = 0.0
		netWheelAcceleration = 0.0
		netEnvironmentalSlowdown = 0.0
		netWheelSlowdown = 0.0
		
		if(train.carriages.none { it.anyAvailableDimensionalCarriage() != null }) return
		
		ensureTrainProps()
		updatePhysicalState()
		
		handleRollingResistance()
		handleGravitationalAcceleration()
		handleBrake()
		handleCurvatureResistance()
		handleAirResistance()
		
		handleSlip()
	}
	
	private inline fun onResult(key: String, value: Double, debug: () -> String = { "" }): Double {
		if(debugEnabled) debugEntries[key] = "${round(value, 10000)} ${debug()}"
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
		
		val preciseMass = config.preciseMass.get()
		
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
			// TODO: make it differ by its type; use CFD or heuristics to compute existing bogies (including SnR bogies)
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
		
		val totalPower = if(powerAmount.isNaN()) config.power.get() else powerAmount
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
		val startingResistance = config.startingResistance.get()
		if(currentSpeed == 0.0) {
			netWheelSlowdown += onResult("rollingResistance", startingResistance)
		} else {
			val p = p ?: return
			var resistance = (p.rollingFactor + p.rolling2Factor * abs(currentSpeed)) / mass * normalMassRatio
			resistance *= config.rollingResistanceMultiplier.get()
			
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
		val accelerationFactor = config.gradientTrainAcceleration.get()
		if(accelerationFactor == 0.0) return
		
		val result = accelerationFactor * gravitationalAcceleration
		netEnvironmentalAcceleration += onResult("gravitationalAcceleration", result)
	}
	
	private fun handleBrake() {
		val factor = config.brakeAcceleration.get()
		
		var brake = targetBreak
		if(config.automaticBrakeAtStation.get() && train.currentStation != null && targetThrottle == 0.0) {
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
		val factor = config.curvatureResistance.get()
		if(factor == 0.0) return
		
		val resistance = CurvatureResistance.calculate(curveRadius)
		val result = factor * resistance * (sqrt(abs(train.speed) + 4) - 2)
		netEnvironmentalSlowdown += onResult("curvatureResistance", result) { "R=${round(curveRadius, 10)}" }
	}
	
	private fun handleAirResistance() {
		val speed = train.speed
		if(speed == 0.0) return
		
		// TODO: tunnel
		val p = p ?: return
		if(!config.airResistance.get()) return
		
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
		// slips do not generally happen, but they might happen where gradient is too large
		if(!config.slipEnabled.get()) {
			slipAmount = 0.0
			return
		}
		
		val friction = config.slipCoefficient.get() * normalMassRatio
		// TODO: slip and underwater / frozen biome / ...etc
		val wheelAcceleration = targetThrottle + netWheelAcceleration - sign(train.speed) * netWheelSlowdown
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
