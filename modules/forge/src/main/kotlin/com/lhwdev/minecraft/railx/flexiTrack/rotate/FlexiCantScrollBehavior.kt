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


class FlexiCantScrollBehavior(be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	FlexiTrackRotateScrollBehavior(kind = FlexiTrackRotateScrollBehaviors.Kind.Cant, be, slot) {
	
	val maxCant = min(Mth.floor(RailXConfig.Server.flexiTrak.maxGradient.get()), 80)
	
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
		val value = (direction.cant * maxCant / PI).roundToInt()
		return when {
			value == 0 -> "0‰"
			value > 0 -> "R$value‰"
			else -> "L${-value}‰"
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
		
		value = maxCant + (direction.cant * maxCant / PI).roundToInt()
		
		return createBoard(
			maxValue = 2 * maxCant - 1,
			title = "Cant",
			formatter = { v ->
				val value = v - maxCant
				when {
					value == 0 -> "0‰"
					value > 0 -> "R${value}‰"
					else -> "L${-value}‰"
				}
			},
		)
	}
	
	override fun formatPreciseDelta(delta: Float): String {
		val value = round(delta / maxCant, 1000)
		return when {
			value == 0f -> "0‰"
			value > 0f -> "R${value}‰"
			else -> "L${-value}‰"
		}
	}
	
	override fun rotateTrack(player: Player, value: Rotation): Boolean {
		val be = be
		val level = be.level!!
		
		val direction = be.block.getNearestTrackDirection(level, be.blockPos, be.blockState, player.lookAngle)
			?.signedAxis ?: return false
		val delta = when(value) {
			is Rotation.Steps -> {
				val initialValue = maxCant + (direction.cant * maxCant / PI).roundToInt()
				if(value.step == initialValue) return false
				(value.step - initialValue) * PI / maxCant
			}
			
			is Rotation.Precise -> value.delta * PI / maxCant
		}
		
		val axis = direction.tangent
		val rotation = Quaterniond().rotationAxis(delta, axis.x, axis.y, axis.z)
		
		applyRotation(
			mapDirection = { direction.applyNormal(rotation.transformUnit(it.normal)).optimize() },
			rotation = rotation,
		)
		return false
	}
}
