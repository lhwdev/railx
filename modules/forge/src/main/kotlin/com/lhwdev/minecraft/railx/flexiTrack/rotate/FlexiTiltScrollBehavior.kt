package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.map
import com.lhwdev.minecraft.railx.flexiTrack.optimize
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import org.joml.Quaterniond
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt


class FlexiTiltScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(Component.literal("Rotate Flexi Track Tilt"), be, slot) {
	override val kind: FlexiTrackRotateScrollBehaviors.Kind
		get() = FlexiTrackRotateScrollBehaviors.Kind.Tilt
	
	val maxTilt = min(Mth.floor(RailXConfig.Server.flexiTrak.maxGradient.asDouble), 80)
	
	override fun formatValue(): String {
		val mc = Minecraft.getInstance()
		val level = be.level!!
		val hitResult = mc.hitResult as? BlockHitResult ?: return "?"
		val direction = be.block.getNearestTrackDirection(
			level,
			hitResult.blockPos,
			level.getBlockState(be.blockPos),
			mc.player!!.lookAngle
		)?.signedAxis ?: return "?"
		val value = (direction.tilt * maxTilt / PI).roundToInt()
		return when {
			value == 0 -> "0‰"
			value > 0 -> "R$value‰"
			else -> "L${-value}‰"
		}
	}
	
	override fun createBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard {
		val level = player.level()
		val direction = be.block.getNearestTrackDirection(
			level,
			hitResult.blockPos,
			level.getBlockState(hitResult.blockPos),
			player.lookAngle
		)?.signedAxis ?: return ValueSettingsBoard(
			Component.literal("Cannot rotate empty track"), 0, 0, emptyList(),
			ValueSettingsFormatter { Component.empty() })
		
		value = maxTilt + (direction.tilt * maxTilt / PI).roundToInt()
		
		return ValueSettingsBoard(
			label,
			2 * maxTilt - 1,
			8,
			listOf(Component.literal("Tilt").withStyle(ChatFormatting.BOLD)),
			ValueSettingsFormatter { v ->
				val value = v.value - maxTilt
				Component.literal(
					when {
						value == 0 -> "0‰"
						value > 0 -> "R${value}‰"
						else -> "L${-value}‰"
					}
				)
			},
		)
	}
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val be = be
		val level = be.level!!
		
		val direction = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?.signedAxis ?: return
		val initialValue = maxTilt + (direction.tilt * maxTilt / PI).roundToInt()
		val delta = valueSetting.value - initialValue
		
		val axis = direction.tangent
		val rotation = Quaterniond().rotationAxis(delta.toDouble() * PI / maxTilt, axis.x, axis.y, axis.z)
		
		be.updateEachConnections {
			val state = be.state
			val newState = state.copy(
				baseShape = state.baseShape.map { direction ->
					direction.applyNormal(rotation.transformUnit(direction.normal).optimize())
						.optimize()
				},
				tilt = state.tilt?.let { tilt -> tilt.copy(axis = rotation.transformUnit(tilt.axis).optimize()) }
			)
			be.updateState(newState)
			
			forEachConnections { connection ->
				val axis = rotation.transformUnit(connection.axes.first).optimize()
				connection.axes.first = axis
				connection.normals.first = rotation.transformUnit(connection.normals.first).optimize()
				connection.starts.first = be.block.getCurveStart(level, be.blockPos, be.blockState, axis)
			}
		}
	}
	
	override fun getClipboardKey(): String =
		"FlexiTrackRotation.Gradient"
}
