package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection.Known.Companion.DivisionCount
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import net.createmod.catnip.math.AngleHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class FlexiDirectionScrollBehavior(
	be: FlexiTrackBlockEntity,
	slot: ValueBoxTransform,
	val axis: FlexiDirection,
) : FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Direction, be, slot) {
	
	override fun getValue(player: Player): Double =
		axis.direction % PI
	
	override val normalRange: ClosedFloatingPointRange<Double>
		get() = 0.0..PI
	
	override val normalStep: Double get() = PI / DivisionCount
	
	override val scrollDirection: ScrollDirection
		get() = ScrollDirection.Descending
	
	override val endInclusive: Boolean
		get() = false
	
	override val coarseStep: Double?
		get() = null
	
	override fun formatRotation(value: Double, precise: Boolean): String {
		return if(precise) {
			val known = FlexiDirection.Known.fromOrNull(radian = value)
			if(known != null) {
				"K${known.ordinal}"
			} else {
				val degrees = AngleHelper.deg(value)
				"${round(degrees, 100)}°"
			}
		} else {
			val known = FlexiDirection.Known.roundFrom(radian = value)
			"K${known.ordinal}"
		}
	}
	
	override fun formatValue(): String {
		val v = valueClient
		val known = FlexiDirection.Known.fromOrNull(radian = v)
		return if(known != null) {
			"K${known.ordinal}"
		} else {
			val degrees = AngleHelper.deg(v)
			"${round(degrees, points = 10)}°"
		}
	}
	
	override fun applyRotation(player: Player, targetValue: Double): Boolean {
		val delta = targetValue - axis.direction
		if(delta similarTo 0.0) return false
		
		val rotation = Quaterniond().rotationY(delta)
		val known = FlexiDirection.Known.fromOrNull(targetValue)
		
		applyRotation(
			mapDirection = { previous ->
				if(known != null && previous closeToUnsigned axis) {
					known.applyNormal(previous.normal)
				} else {
					val h = previous.tangent.horizontalDistance()
					val newTangent = Vec3(h * cos(targetValue), previous.tangent.y, -h * sin(targetValue))
					val newNormal = rotation.transformUnit(previous.normal)
					FlexiDirection.Two(tangent = newTangent, normal = newNormal).optimize()
				}
			},
			rotation = rotation,
		)
		return true
	}
}

