package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.map
import com.lhwdev.minecraft.railx.flexiTrack.optimize
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import kotlin.math.PI
import kotlin.math.roundToInt


class FlexiDirectionScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform, val axis: FlexiDirection) :
	FlexiTrackRotateScrollBehavior(Component.literal("Rotate Flexi Track Direction"), be, slot) {
	override val kind: FlexiTrackRotateScrollBehaviors.Kind
		get() = FlexiTrackRotateScrollBehaviors.Kind.Direction
	
	
	val offset = if(axis is FlexiDirection.Known) {
		axis.ordinal
	} else {
		FlexiDirection.Known.roundFrom(axis.tangent).ordinal
	}
	
	init {
		value = offset
	}
	
	override fun formatValue(): String {
		return if(axis is FlexiDirection.Known) {
			"K${axis.ordinal}"
		} else {
			"${((axis.direction * 180 / PI + 360) % 360).roundToInt()}°"
		}
	}
	
	override fun createBoard(player: Player, hitResult: BlockHitResult) = ValueSettingsBoard(
		label,
		FlexiDirection.Known.DivisionCount,
		8,
		listOf(Component.literal("Direction").withStyle(ChatFormatting.BOLD)),
		ValueSettingsFormatter { v ->
			val value = v.value
			if(axis is FlexiDirection.Known) {
				Component.literal("K$value")
			} else {
				val angle = ((axis.rotateKnown(value).tangentAngle * 180 / PI + 360) % 360).roundToInt()
				Component.literal("${angle}°")
			}
		},
	)
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val be = be
		val level = be.level!!
		val delta = -(valueSetting.value - offset)
		
		be.updateEachConnections {
			be.updateState(be.state.copy(baseShape = be.shape.map { direction -> direction.rotateKnown(delta) }))
			
			forEachConnections { connection ->
				val yaw = delta.toFloat() / FlexiDirection.Known.DivisionCount * PI.toFloat()
				val axis = connection.axes.first.yRot(yaw).optimize()
				connection.axes.first = axis
				connection.normals.first = connection.normals.first.yRot(yaw).optimize()
				connection.starts.first = be.block.getCurveStart(level, be.blockPos, be.blockState, axis)
			}
		}
	}
	
	override fun getClipboardKey(): String =
		"FlexiTrackRotation.Direction"
}
