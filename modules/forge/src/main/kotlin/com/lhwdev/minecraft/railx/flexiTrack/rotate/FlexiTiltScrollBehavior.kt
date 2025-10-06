package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.map
import com.lhwdev.minecraft.railx.utils.transform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import org.joml.Quaterniond
import kotlin.math.PI


class FlexiTiltScrollBehavior(label: Component, be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(label, be, slot) {
	val maxTilt = Mth.floor(RailXConfig.Server.flexiTrak.maxGradient.asDouble)
	
	override fun createBoard(player: Player, hitResult: BlockHitResult) = ValueSettingsBoard(
		label,
		2 * maxTilt - 1,
		8,
		listOf(Component.literal("Tilt").withStyle(ChatFormatting.BOLD)),
		ValueSettingsFormatter { v ->
			val value = v.value - maxTilt
			Component.literal(if(value >= 0) "R${value}‰" else "L${-value}‰")
		},
	)
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val be = be
		val level = be.level!!
		val delta = valueSetting.value - maxTilt
		
		val direction = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?: return
		val axis = direction.tangent
		val rotation = Quaterniond().rotationAxis(delta.toDouble() * PI / maxTilt, axis.x, axis.y, axis.z)
		be.updateState(be.state.copy(baseShape = be.shape.map { direction ->
			direction.applyNormal(rotation.transform(direction.normal))
		}))
		
		be.updateEachConnections { connection ->
			val axis = rotation.transform(connection.axes.first)
			connection.axes.first = axis
			connection.normals.first = rotation.transform(connection.normals.first)
			connection.starts.first = be.block.getCurveStart(level, be.blockPos, be.blockState, axis)
		}
	}
	
	override fun getClipboardKey(): String =
		"FlexiTrackRotation.Gradient"
}