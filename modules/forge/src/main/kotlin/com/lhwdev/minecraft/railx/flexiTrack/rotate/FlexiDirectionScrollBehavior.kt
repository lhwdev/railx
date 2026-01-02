package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection.Known.Companion.DivisionCount
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.signToString
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import org.joml.Quaterniond
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt


class FlexiDirectionScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform, val axis: FlexiDirection) :
	FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Direction, be, slot) {
	
	val offset = if(axis is FlexiDirection.Known) {
		axis.ordinal
	} else {
		FlexiDirection.Known.roundFrom(axis.tangent).ordinal
	}
	
	override fun formatValue(): String {
		return if(axis is FlexiDirection.Known) {
			"K${axis.ordinal}"
		} else {
			"${((axis.direction * 180 / PI + 360) % 360).roundToInt()}°"
		}
	}
	
	override fun createRotationBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard {
		value = DivisionCount - offset
		return createBoard(
			maxValue = DivisionCount,
			title = "Direction \u27f3",
			formatter = { value -> "K${DivisionCount - value}" },
		)
	}
	
	override fun formatPreciseDelta(delta: Float): String {
		val delta = -delta * PI / DivisionCount
		val angle = round((abs(delta) * 180 / PI + 360) % 360, 100)
		return "${delta.signToString()}${angle}°"
	}
	
	
	override fun rotateTrack(player: Player, value: Rotation): Boolean {
		when(value) {
			is Rotation.Steps -> {
				val delta = (DivisionCount - value.step) - offset
				if(delta == 0) return false
				
				val direction = FlexiDirection.Known.fromOrdinal(DivisionCount - value.step)
				val rotation = Quaterniond().rotationY(direction.tangentAngle - axis.tangentAngle)
				applyRotation(
					mapDirection = { direction },
					rotation = rotation,
				)
			}
			
			is Rotation.Precise -> {
				val delta = -value.delta * PI / DivisionCount
				applyRotation(
					mapDirection = { it.rotate(byRadian = delta.toFloat()) },
					rotation = Quaterniond().rotationY(delta),
				)
			}
		}
		return true
	}
}
