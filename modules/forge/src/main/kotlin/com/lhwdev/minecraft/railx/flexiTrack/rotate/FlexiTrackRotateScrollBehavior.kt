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
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniondc
import kotlin.math.roundToInt


private fun FlexiTrackRotateScrollBehaviors.Kind.createLabel() =
	Component.literal("Rotate Flexi Track ")
		.append(Component.literal(title))

private fun FlexiTrackRotateScrollBehaviors.Kind.createScreenLabel() =
	Component.literal("Set Flexi Track ")
		.append(Component.literal(title).withStyle(ChatFormatting.YELLOW))

abstract class FlexiTrackRotateScrollBehavior(
	val kind: FlexiTrackRotateScrollBehaviors.Kind,
	be: FlexiTrackBlockEntity,
	slot: ValueBoxTransform,
) : ScrollValueBehaviour(kind.createLabel(), be, slot) {
	
	companion object {
		const val PreciseStepCount = 100
	}
	
	enum class RotateStepMode {
		Normal,
		Precise,
		Coarse;
		
		companion object {
			fun client(): RotateStepMode = when {
				Screen.hasControlDown() -> Precise
				Screen.hasAltDown() -> Coarse
				else -> Normal
			}
		}
	}
	
	enum class ScrollDirection { Ascending, Descending }
	
	protected val be: FlexiTrackBlockEntity
		get() = blockEntity as FlexiTrackBlockEntity
	
	abstract fun getValue(player: Player): Double
	
	val valueClient: Double
		get() = getValue(player = Minecraft.getInstance().player!!)
	
	open val modeClient: RotateStepMode
		get() = RotateStepMode.client()
	
	open val scrollDirection: ScrollDirection
		get() = ScrollDirection.Ascending
	
	abstract val normalRange: ClosedFloatingPointRange<Double>
	
	open val coarseRange: ClosedFloatingPointRange<Double>
		get() = normalRange
	
	open val endInclusive: Boolean
		get() = true
	
	abstract val normalStep: Double
	
	open val coarseStep: Double?
		get() = (coarseRange.width / 10.0).coerceAtLeast(normalStep)
	
	val preciseStep: Double
		get() = normalStep / PreciseStepCount
	
	abstract fun formatRotation(value: Double, precise: Boolean): String
	
	abstract fun applyRotation(player: Player, targetValue: Double): Boolean
	
	
	private fun getPreciseMinValue(value: Double): Double {
		val currentStep = getValueStep(value, stepWidth = preciseStep) / PreciseStepCount
		return normalRange.start + currentStep * normalStep
	}
	
	private fun getValueStep(value: Double, minValue: Double, stepWidth: Double, steps: Int = Int.MAX_VALUE): Int =
		((value - minValue) / stepWidth)
			.roundToInt()
			.coerceIn(0, steps)
	
	private fun getValueStep(
		value: Double,
		range: ClosedFloatingPointRange<Double> = normalRange,
		stepWidth: Double,
	): Int {
		val steps = (range.width / stepWidth).roundToInt()
		return getValueStep(value = value, minValue = range.start, stepWidth = stepWidth, steps = steps)
	}
	
	fun createBoard(player: Player, hitResult: BlockHitResult, mode: RotateStepMode): ValueSettingsBoard {
		val current = getValue(player)
		if(mode == RotateStepMode.Precise) {
			val preciseMinValue = getPreciseMinValue(current)
			value = getValueStep(
				value = current,
				minValue = preciseMinValue,
				stepWidth = preciseStep,
				steps = PreciseStepCount
			)
			
			return ValueSettingsBoard(
				kind.createScreenLabel(),
				PreciseStepCount,
				10,
				listOf(Component.literal(kind.title).withStyle(ChatFormatting.BOLD)),
				ValueSettingsFormatter { valueSettings ->
					val v = preciseMinValue + valueSettings.value * preciseStep
					Component.literal(formatRotation(v, precise = true))
				}
			)
		}
		
		val coarseStep = coarseStep
		val coarse = mode == RotateStepMode.Coarse && coarseStep != null
		val range = if(coarse) coarseRange else normalRange
		val step = if(coarse) coarseStep else normalStep
		val steps = (range.width / step).roundToInt() // assuming valueWidth is multiplier of step (mathematically)
		
		value = getValueStep(value = current, minValue = range.start, stepWidth = step, steps = steps)
		
		return ValueSettingsBoard(
			kind.createScreenLabel(),
			if(endInclusive) steps else steps - 1,
			8,
			listOf(Component.literal(kind.title).withStyle(ChatFormatting.BOLD)),
			ValueSettingsFormatter { valueSettings ->
				val v = range.start + valueSettings.value * step
				Component.literal(formatRotation(v, precise = false))
			}
		)
		
	}
	
	override fun createBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard =
		createBoard(player, hitResult, mode = modeClient)
	
	override fun formatValue(): String =
		formatRotation(valueClient, precise = false)
	
	fun setValueSettings(player: Player, value: Int, mode: RotateStepMode): Boolean {
		val targetValue = when(mode) {
			RotateStepMode.Precise -> {
				val preciseMinValue = getPreciseMinValue(getValue(player))
				preciseMinValue + value * preciseStep
			}
			
			RotateStepMode.Normal -> normalRange.start + value * normalStep
			RotateStepMode.Coarse -> coarseRange.start + value * (coarseStep ?: normalStep)
		}
		
		val handled = applyRotation(player, targetValue)
		return handled
	}
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		val handled = setValueSettings(
			player,
			value = valueSetting.value,
			mode = if(ctrlDown) RotateStepMode.Precise else RotateStepMode.Normal
		)
		
		if(handled) super.setValueSettings(player, valueSetting, ctrlDown)
	}
	
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
			
			val ends = mutableListOf<Vec3>()
			
			for(axis in be.shape.axes) {
				ends += axis.tangent
				ends += -axis.tangent
			}
			
			forEachConnections { connection ->
				val axis = rotation.transformUnit(connection.axes.first).optimize()
				var maxDot = Double.NEGATIVE_INFINITY
				var maxAxis = ends[0]
				for(end in ends) {
					val distance = end.dot(axis)
					if(distance > maxDot) {
						maxDot = distance
						maxAxis = end
					}
				}
				
				if(maxDot < 0.99) {
					println("Expected matching axis in previous connection")
				}
				
				connection.axes.first = maxAxis
				connection.starts.first = track.getCurveStart(level, be.blockPos, be.blockState, maxAxis)
				connection.normals.first = rotation.transformUnit(connection.normals.first).optimize()
			}
		}
	}
}


internal val ClosedFloatingPointRange<Double>.width: Double
	get() = endInclusive - start
