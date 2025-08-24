package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult


class FlexiTrackRotateScrollBehavior(label: Component, be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	ScrollValueBehaviour(label, be, slot) {
	
	protected val be: FlexiTrackBlockEntity
		get() = blockEntity as FlexiTrackBlockEntity
	
	override fun createBoard(
		player: Player,
		hitResult: BlockHitResult,
	): ValueSettingsBoard {
		val rows = listOf(
			Component.literal("direction").withStyle(ChatFormatting.BOLD),
			Component.literal("gradient").withStyle(ChatFormatting.BOLD),
			Component.literal("tilt").withStyle(ChatFormatting.BOLD),
		)
		val formatter = ValueSettingsFormatter { v -> Component.literal("${v.value}") }
		return ValueSettingsBoard(
			label,
			FlexiDirection.Known.DivisionCount - 1,
			FlexiDirection.Known.DivisionCount / 4,
			rows,
			formatter,
		)
	}
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val value = valueSetting.value.coerceIn(0, max).toDouble() / FlexiDirection.Known.DivisionCount
		// TODO
		val newAxes = be.shape.axes.map {
			val rotation = it.toRotation()
			when(valueSetting.row) {
				0 -> rotation.copy(direction = value)
				1 -> rotation.copy(direction = value)
				2 -> rotation.copy(direction = value)
				else -> error("no")
			}
		}
	}
	
	override fun getClipboardKey(): String =
		"FlexiTrackRotation"
}
