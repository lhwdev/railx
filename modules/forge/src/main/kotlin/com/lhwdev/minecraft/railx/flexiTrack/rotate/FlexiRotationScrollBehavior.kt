package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.map
import com.lhwdev.minecraft.railx.flexiTrack.optimize
import com.lhwdev.minecraft.railx.flexiTrack.tangentAngle
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


class FlexiRotationScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(Component.literal("Rotate Flexi Track Direction"), be, slot) {
	init {
		value = FlexiDirection.Known.DivisionCount / 2
	}
	
	override val kind: FlexiTrackRotateScrollBehaviors.Kind
		get() = FlexiTrackRotateScrollBehaviors.Kind.Direction
	
	override fun formatValue(): String = "F"
	
	override fun createBoard(player: Player, hitResult: BlockHitResult) = ValueSettingsBoard(
		label,
		FlexiDirection.Known.DivisionCount,
		8,
		listOf(Component.literal("Rotation").withStyle(ChatFormatting.BOLD)),
		ValueSettingsFormatter { v ->
			val value = v.value - FlexiDirection.Known.DivisionCount / 2
			Component.literal(if(value >= 0) "+K$value" else "-K${-value}")
		},
	)
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val be = be
		val level = be.level!!
		val delta = -(valueSetting.value - FlexiDirection.Known.DivisionCount / 2)
		
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