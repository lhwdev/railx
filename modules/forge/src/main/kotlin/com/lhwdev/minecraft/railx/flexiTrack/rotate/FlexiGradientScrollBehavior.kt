package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import kotlin.math.atan
import kotlin.math.roundToInt


class FlexiGradientScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Gradient, be, slot) {
	
	companion object {
		const val MaxNormalValue = 100.0
		
		const val MinCoarseStepWidth = 4.0
		const val MinCoarseSteps = 4
		const val MaxCoarseSteps = 100
	}
	
	override fun getValue(player: Player): Double {
		val level = be.level ?: return 0.0
		val lookAngle = player.lookAngle
		val direction = be.block
			.getNearestTrackDirection(level, be.blockPos, be.blockState, lookAngle)
			?.signedAxis ?: return 0.0
		return direction.gradientSlope * 1000.0
	}
	
	override val normalRange: ClosedFloatingPointRange<Double>
		get() = RailXConfig.Server.flexiTrak.maxGradient.get()
			.coerceAtMost(MaxNormalValue)
			.let { -it..it }
	
	override val coarseRange: ClosedFloatingPointRange<Double>
		get() = RailXConfig.Server.flexiTrak.maxGradient.get()
			.coerceAtMost(MaxNormalValue * MaxCoarseSteps / 2) // MaxCoarseStepWidth == MaxNormalValue
			.let { -it..it }
	
	override val normalStep: Double get() = 1.0
	
	override val coarseStep: Double?
		get() {
			val width = coarseRange.width
			return (width / MaxCoarseSteps).coerceAtLeast(MinCoarseStepWidth)
				.takeIf { width / it >= MinCoarseSteps }
		}
	
	override fun formatRotation(value: Double, precise: Boolean): String {
		return if(precise) {
			val rounded = round(value, 100)
			when {
				rounded == 0.0 -> "0.00‰"
				rounded > 0.0 -> "+${rounded}‰"
				else -> "-${-rounded}‰"
			}
		} else {
			val intVal = value.roundToInt()
			when {
				intVal == 0 -> "0‰"
				intVal > 0 -> "+$intVal‰"
				else -> "-${-intVal}‰"
			}
		}
	}
	
	override fun formatValue(): String {
		val rounded = round(valueClient, 10)
		val integer = rounded.toInt()
		if(rounded == integer.toDouble()) return when {
			integer == 0 -> "0‰"
			integer > 0 -> "+${integer}‰"
			else -> "-${-integer}‰"
		}
		
		return when {
			rounded == 0.0 -> "0.0‰"
			rounded > 0.0 -> "+${rounded}‰"
			else -> "-${-rounded}‰"
		}
	}
	
	override fun applyRotation(player: Player, targetValue: Double): Boolean {
		val level = be.level ?: return false
		val direction = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?.signedAxis ?: return false
		
		val targetSlope = targetValue / 1000.0
		val axis = direction.tangent.cross(direction.normal)
		val delta = atan(targetSlope) - direction.gradient
		val rotation = Quaterniond().rotationAxis(delta, axis.x, axis.y, axis.z)
		
		val tangent = direction.tangent
		val horizontalDist = tangent.horizontalDistance()
		val newTangent = Vec3(tangent.x, horizontalDist * targetSlope, tangent.z).normalize()
		val newNormal = rotation.transformUnit(direction.normal)
		
		applyRotation(
			mapDirection = {
				FlexiDirection.Two(
					tangent = newTangent,
					normal = newNormal,
				).optimize()
			},
			rotation = rotation,
		)
		return true
	}
}
