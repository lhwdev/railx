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


class FlexiGradientScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(Component.literal("Rotate Flexi Track Gradient"), be, slot) {
	override val kind: FlexiTrackRotateScrollBehaviors.Kind
		get() = FlexiTrackRotateScrollBehaviors.Kind.Gradient
	
	val maxGradient = min(Mth.floor(RailXConfig.Server.flexiTrak.maxGradient.asDouble), 80)
	
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
		val value = (direction.gradient * maxGradient / PI).roundToInt()
		return when {
			value == 0 -> "0‰"
			value > 0 -> "+$value‰"
			else -> "-${-value}‰"
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
		
		value = maxGradient + (direction.gradient * maxGradient / PI).roundToInt()
		
		return ValueSettingsBoard(
			label,
			2 * maxGradient - 1,
			8,
			listOf(Component.literal("Gradient").withStyle(ChatFormatting.BOLD)),
			ValueSettingsFormatter { v ->
				val value = v.value - maxGradient
				Component.literal(
					when {
						value == 0 -> "0‰"
						value > 0 -> "+${value}‰"
						else -> "-${-value}‰"
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
		
		val directionAxis = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?: return
		val direction = directionAxis.signedAxis
		val initialValue = maxGradient + (direction.gradient * maxGradient / PI).roundToInt()
		val delta = valueSetting.value - initialValue
		
		val axis = direction.tangent.cross(direction.normal)
		val rotation = Quaterniond().rotationAxis(delta.toDouble() * PI / maxGradient, axis.x, axis.y, axis.z)
		
		be.updateEachConnections {
			val state = be.state
			val newState = state.copy(
				baseShape = state.baseShape.map { it.applyNormal(rotation.transformUnit(it.normal).optimize()) },
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
