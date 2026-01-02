package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.map
import com.lhwdev.minecraft.railx.flexiTrack.optimize
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniondc


private fun FlexiTrackRotateScrollBehaviors.Kind.createLabel() =
	Component.literal("Rotate Flexi Track ")
		.append(Component.literal(title))

private fun FlexiTrackRotateScrollBehaviors.Kind.createScreenLabel(precise: Boolean) =
	Component.literal(if(precise) "Rotate Flexi Track " else "Set Flexi Track ")
		.append(Component.literal(title).withStyle(ChatFormatting.YELLOW))


private const val Steps = 32

abstract class FlexiTrackRotateScrollBehavior(
	val kind: FlexiTrackRotateScrollBehaviors.Kind,
	be: FlexiTrackBlockEntity,
	slot: ValueBoxTransform,
) : ScrollValueBehaviour(kind.createLabel(), be, slot) {
	
	sealed class Rotation {
		class Steps(val step: Int) : Rotation()
		class Precise(val delta: Float) : Rotation()
	}
	
	
	protected val be: FlexiTrackBlockEntity
		get() = blockEntity as FlexiTrackBlockEntity
	
	protected val isPrecise: Boolean
		get() = Screen.hasControlDown()
	
	
	final override fun createBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard {
		val board = createRotationBoard(player, hitResult)
		
		if(isPrecise) {
			value = Steps
			return ValueSettingsBoard(
				board.title,
				Steps * 2,
				8,
				board.rows,
				ValueSettingsFormatter { Component.literal(formatPreciseDelta((it.value - Steps) / Steps.toFloat())) },
			)
		}
		return board
	}
	
	
	protected abstract fun createRotationBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard
	
	protected abstract fun formatPreciseDelta(delta: Float): String
	
	protected fun createBoard(
		maxValue: Int,
		title: String,
		formatter: (Int) -> String,
	): ValueSettingsBoard = ValueSettingsBoard(
		kind.createScreenLabel(precise = isPrecise),
		maxValue,
		8,
		listOf(Component.literal(title).withStyle(ChatFormatting.BOLD)),
		ValueSettingsFormatter { Component.literal(formatter(it.value)) },
	)
	
	final override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val step = valueSetting.value
		val value = if(isPrecise) {
			val delta = step - Steps
			if(delta == 0) return
			Rotation.Precise(delta = delta / Steps.toFloat())
		} else {
			Rotation.Steps(step = step)
		}
		
		val handled = rotateTrack(player, value)
		if(handled) {
			super.setValueSettings(player, valueSetting, ctrlDown)
		}
	}
	
	abstract fun rotateTrack(player: Player, value: Rotation): Boolean
	
	protected fun applyRotation(
		mapDirection: (FlexiDirection) -> FlexiDirection,
		rotation: Quaterniondc,
	) {
		val be = be
		val track = be.block
		val level = be.level!!
		
		be.updateEachConnections {
			val state = be.state
			val newState = state.copy(
				baseShape = state.baseShape.map(mapDirection),
				tilt = state.tilt?.let { tilt -> tilt.copy(axis = rotation.transformUnit(tilt.axis).optimize()) },
			)
			be.updateState(newState)
			
			val center = track.getTrackBase(level, be.blockPos, be.blockState)
			val ends = mutableListOf<Vec3>()
			
			for(axis in be.shape.axes) {
				ends += axis.tangent
				ends += -axis.tangent
			}
			
			forEachConnections { connection ->
				val axis = rotation.transformUnit(connection.axes.first).optimize()
				var maxDistance = Double.POSITIVE_INFINITY
				var maxAxis = ends[0]
				for(end in ends) {
					val distance = end.dot(axis)
					if(distance < maxDistance) {
						maxDistance = distance
						maxAxis = end
					}
				}
				
				if(maxDistance > 0.005) {
					println("why so far? breakpoint here")
				}
				
				connection.axes.first = maxAxis
				connection.starts.first = track.getCurveStart(level, be.blockPos, be.blockState, maxAxis)
				connection.normals.first = rotation.transformUnit(connection.normals.first).optimize()
			}
		}
	}
}
