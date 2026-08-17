package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import net.minecraft.world.entity.player.Player
import org.joml.Quaterniond
import kotlin.math.PI
import kotlin.math.roundToInt


class FlexiCantScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Cant, be, slot) {
	
	override fun getValue(player: Player): Double {
		val level = be.level ?: return 0.0
		val lookAngle = player.lookAngle
		val direction = be.block
			.getNearestTrackDirection(level, be.blockPos, be.blockState, lookAngle)
			?.signedAxis ?: return 0.0
		return direction.cant * 180.0 / PI
	}
	
	override val normalRange: ClosedFloatingPointRange<Double>
		get() = RailXConfig.Server.flexiTrak.maxCant.get()
			.coerceAtMost(50.0)
			.let { -it..it }
	
	override val coarseRange: ClosedFloatingPointRange<Double>
		get() = RailXConfig.Server.flexiTrak.maxCant.get()
			.let { -it..it }
	
	override val normalStep: Double get() = 1.0
	
	override val coarseStep: Double?
		get() {
			val range = coarseRange
			if(range.endInclusive <= 50.0) return null
			return 10.0
		}
	
	override fun formatRotation(value: Double, precise: Boolean): String = if(precise) {
		val v = round(value, 100)
		when {
			v == 0.0 -> "0.00°"
			v > 0.0 -> "R${v}°"
			else -> "L${-v}°"
		}
	} else {
		val v = value.roundToInt()
		when {
			v == 0 -> "0°"
			v > 0 -> "R${v}°"
			else -> "L${-v}°"
		}
	}
	
	override fun applyRotation(player: Player, targetValue: Double): Boolean {
		val level = be.level ?: return false
		val direction = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?.signedAxis ?: return false
		
		val targetCantRad = targetValue * PI / 180.0
		val delta = targetCantRad - direction.cant
		val axis = direction.tangent
		val rotation = Quaterniond().rotationAxis(delta, axis.x, axis.y, axis.z)
		
		applyRotation(
			mapDirection = { direction.applyNormal(rotation.transformUnit(it.normal)).optimize() },
			rotation = rotation,
		)
		return true
	}
}
