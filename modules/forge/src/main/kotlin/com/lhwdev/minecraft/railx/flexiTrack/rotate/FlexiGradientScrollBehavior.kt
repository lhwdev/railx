package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
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
	FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Gradient, be, slot) {
	
	val maxGradient = min(Mth.floor(RailXConfig.Server.flexiTrak.maxGradient.get()), 80)
	
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
	
	
	override fun createRotationBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard {
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
		
		return createBoard(
			maxValue = 2 * maxGradient - 1,
			title = "Gradient",
			formatter = { v ->
				val value = v - maxGradient
				when {
					value == 0 -> "0‰"
					value > 0 -> "+${value}‰"
					else -> "-${-value}‰"
				}
			},
		)
	}
	
	override fun formatPreciseDelta(delta: Float): String =
		"${round(delta, 1000)}‰"
	
	
	override fun rotateTrack(player: Player, value: Rotation): Boolean {
		val be = be
		val level = be.level!!
		
		val directionAxis = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?: return false
		val direction = directionAxis.signedAxis
		val delta = when(value) {
			is Rotation.Steps -> {
				val initialValue = maxGradient + (direction.gradient * maxGradient / PI).roundToInt()
				if(value.step == initialValue) return false
				(value.step - initialValue) * PI / maxGradient
			}
			
			is Rotation.Precise -> value.delta * PI / maxGradient
		}
		
		val axis = direction.tangent.cross(direction.normal)
		val rotation = Quaterniond().rotationAxis(delta, axis.x, axis.y, axis.z)
		
		applyRotation(
			mapDirection = { it.applyNormal(rotation.transformUnit(it.normal)).optimize() },
			rotation = rotation,
		)
		return true
	}
}
