package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection.Known.Companion.DivisionCount
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.map
import com.lhwdev.minecraft.railx.utils.math.*
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import kotlin.math.PI


class FlexiDirectionScrollBehavior(
	be: FlexiTrackBlockEntity,
	slot: ValueBoxTransform,
	val axis: FlexiDirection,
) : FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Direction, be, slot) {
	
	override fun getValue(player: Player): Double =
		axis.direction.radian % PI
	
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
		val v = value.radian
		return if(precise) {
			val known = FlexiDirection.Known.fromOrNull(direction = v)
			if(known != null) {
				"K${known.ordinal}"
			} else {
				"${round(v.degree, 100)}°"
			}
		} else {
			val known = FlexiDirection.Known.roundFrom(direction = v)
			"K${known.ordinal}"
		}
	}
	
	override fun formatValue(): String {
		val v = valueClient.radian
		val known = FlexiDirection.Known.fromOrNull(direction = v)
		return if(known != null) {
			"K${known.ordinal}"
		} else {
			"${round(v.degree, points = 10)}°"
		}
	}
	
	override fun applyRotation(player: Player, targetValue: Double): Boolean {
		val v = targetValue.radian
		val delta = targetValue - axis.direction.radian
		if(delta similarTo 0.0) return false
		
		val rotation = Quaterniond().rotationY(delta)
		val known = FlexiDirection.Known.fromOrNull(v)
		
		applyRotation(
			mapState = { state ->
				state.copy(
					baseShape = state.baseShape.map { previous ->
						if(known != null && previous closeToUnsigned axis) {
							known.applyNormal(previous.normal)
						} else {
							val h = previous.tangent.horizontalDistance()
							val newTangent = Vec3(h * cos(v), previous.tangent.y, -h * sin(v))
							val newNormal = rotation.transformUnit(previous.normal)
							FlexiDirection.Two(tangent = newTangent, normal = newNormal)
								.optimize()
						}
					},
					tilt = state.tilt?.transformAxis(rotation),
				)
				
			},
			rotation = rotation,
		)
		return true
	}
}

