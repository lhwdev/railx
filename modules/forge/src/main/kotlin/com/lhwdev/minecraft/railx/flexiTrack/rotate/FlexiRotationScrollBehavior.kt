package com.lhwdev.minecraft.railx.flexiTrack.rotate

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


class FlexiRotationScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Direction, be, slot) {
	
	override fun formatValue(): String = "F"
	
	override fun createRotationBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard {
		value = DivisionCount / 2
		return createBoard(
			maxValue = DivisionCount,
			title = "Rotation \u27f3",
			formatter = {
				val value = it - DivisionCount / 2
				if(value >= 0) "+K$value" else "-K${-value}"
			},
		)
	}
	
	override fun formatPreciseDelta(delta: Float): String {
		val deltaRadian = delta * PI / DivisionCount
		val angle = round((abs(deltaRadian) * 180 / PI + 360) % 360, 100)
		return "${deltaRadian.signToString()}${angle}°"
	}
	
	override fun rotateTrack(player: Player, value: Rotation): Boolean {
		when(value) {
			is Rotation.Steps -> {
				val delta = value.step - DivisionCount / 2
				if(delta == 0) return false
				val rotation = Quaterniond().rotationY(delta.toDouble() / DivisionCount * PI)
				applyRotation(
					mapDirection = { it.rotateKnown(by = delta) },
					rotation = rotation,
				)
			}
			
			is Rotation.Precise -> {
				val delta = value.delta.toDouble() / DivisionCount * PI
				applyRotation(
					mapDirection = { it.rotate(byRadian = delta.toFloat()) },
					rotation = Quaterniond().rotationY(delta),
				)
			}
		}
		return true
	}
}
