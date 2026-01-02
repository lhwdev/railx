package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.common.ScrollValueBehaviorExtension
import com.lhwdev.minecraft.railx.common.ValueSettingsBehaviourExtra
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiShape
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockBehavior
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.utils.transformUnit
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Quaternionfc
import java.util.function.Consumer
import kotlin.math.PI


class FlexiTrackRotateScrollBehaviors(be: FlexiTrackBlockEntity) :
	ScrollValueBehaviour(Component.literal("Rotate Flexi Track"), be, FlexiRotationValueBox),
	FlexiTrackBlockBehavior, ValueSettingsBehaviourExtra, ScrollValueBehaviorExtension {
	
	enum class Kind(val title: String) {
		Direction(title = "Direction"),
		Gradient(title = "Gradient"),
		Tilt(title = "Tilt");
		
		fun next(): Kind = entries[(ordinal + 1) % entries.size]
	}
	
	companion object {
		var currentKind: Kind = Kind.Direction
	}
	
	init {
		requiresWrench()
	}
	
	private val be: FlexiTrackBlockEntity
		get() = blockEntity as FlexiTrackBlockEntity
	
	private var delegate: FlexiTrackRotateScrollBehavior? = null
	
	private fun delegate() = delegate?.takeIf { it.kind == currentKind }
		?: createDelegate().also {
			delegate = it
			label = it.label
		}
	
	private fun createDelegate() = when(currentKind) {
		Kind.Direction -> be.shape.axes.singleOrNull()?.let { FlexiDirectionScrollBehavior(be, slotPositioning, it) }
			?: FlexiRotationScrollBehavior(be, slotPositioning)
		
		Kind.Gradient -> FlexiGradientScrollBehavior(be, slotPositioning)
		Kind.Tilt -> FlexiTiltScrollBehavior(be, slotPositioning)
	}
	
	override fun onFlexiStateUpdate(level: LevelReader, pos: BlockPos) {
		delegate = null
	}
	
	override fun onShortInteract(player: Player, hand: InteractionHand, side: Direction, hitResult: BlockHitResult) {
		currentKind = currentKind.next()
	}
	
	override fun isActive(): Boolean =
		delegate().isActive
	
	
	override fun createBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard =
		delegate().createBoard(player, hitResult)
	
	override fun getValue(): Int = delegate().value
	
	override fun setValue(value: Int) {
		delegate().value = value
	}
	
	override fun formatValue(): String =
		delegate().formatValue()
	
	override fun getValueSettings(): ValueSettingsBehaviour.ValueSettings =
		delegate().valueSettings
	
	override fun setValueSettings(
		player: Player,
		valueSetting: ValueSettingsBehaviour.ValueSettings,
		ctrlDown: Boolean,
	) {
		delegate().setValueSettings(player, valueSetting, ctrlDown)
	}
	
	
	override fun createBoardScreen(
		pos: BlockPos,
		board: ValueSettingsBoard,
		valueSettings: ValueSettingsBehaviour.ValueSettings,
		onHover: Consumer<ValueSettingsBehaviour.ValueSettings>,
		netId: Int,
	): ValueSettingsScreen? {
		return FlexiTrackRotateScreen(
			behavior = this,
			hitResult = Minecraft.getInstance().hitResult as? BlockHitResult ?: return null,
			pos, board, valueSettings, onHover, netId,
		)
	}
	
	override fun addExtraTips(to: MutableList<MutableComponent>) {
		fun kindText(kind: Kind) = Component.literal(kind.name.first().uppercase() + kind.name.drop(1))
			.withStyle(if(currentKind == kind) ChatFormatting.GREEN else ChatFormatting.GRAY)
		
		to += Component.literal("Short click to cycle between ")
			.append(kindText(Kind.Direction))
			.append(", ")
			.append(kindText(Kind.Gradient))
			.append(", and ")
			.append(kindText(Kind.Tilt))
			.append(".")
	}
	
	override fun read(nbt: CompoundTag, clientPacket: Boolean) {}
	override fun write(nbt: CompoundTag, clientPacket: Boolean) {}
	
	override fun writeToClipboard(tag: CompoundTag, side: Direction): Boolean {
		val axis = be.shape.axes.singleOrNull() ?: return false
		tag.put("railx:FlexiTrackDirection", axis.write())
		return true
	}
	
	override fun readFromClipboard(
		tag: CompoundTag,
		player: Player,
		side: Direction,
		simulate: Boolean,
	): Boolean {
		val state = be.state
		if(state.shape.axes.size != 1) return false
		val direction = tag.get("railx:FlexiTrackDirection") as? CompoundTag ?: return false
		if(simulate) return true
		
		be.updateState(state.copy(baseShape = FlexiShape.Single(axis = FlexiDirection.read(direction))))
		return true
	}
	
	override fun getClipboardKey(): String =
		"FlexiTrackDirection"
}

private object FlexiRotationValueBox : ValueBoxTransform.Sided() {
	override fun getSouthLocation(): Vec3 = Vec3.ZERO
	
	override fun isSideActive(state: BlockState, direction: Direction): Boolean =
		direction == Direction.UP
	
	fun getRotation(level: LevelAccessor, pos: BlockPos): Quaternionf? {
		val blockEntity = level.getBlockEntity(pos) as? FlexiTrackBlockEntity ?: return null
		val direction = blockEntity.state.shape.axis1
		val directionCache = blockEntity.state.shapeCache.firstOrNull() ?: return null
		val player = Minecraft.getInstance().player ?: return null
		val sign = player.lookAngle.dot(direction.tangent)
		
		val halfPi = PI.toFloat() * 0.5f
		return Quaternionf(directionCache.rotationValue)
			.rotateY(if(sign < 0) halfPi * 3 else halfPi)
	}
	
	fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState, rotation: Quaternionfc): Vec3? {
		val shape = state.getShape(level, pos)
		val height = shape.max(Direction.Axis.Y) - shape.min(Direction.Axis.Y)
		
		return rotation.transformUnit(Vec3(0.0, height, 0.0))
			.add(0.5, 0.0, 0.5)
	}
	
	override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3? {
		val rotation = getRotation(level, pos) ?: return null
		return getLocalOffset(level, pos, state, rotation)
	}
	
	override fun transform(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
		val rotation = getRotation(level, pos) ?: return
		val localOffset = getLocalOffset(level, pos, state, rotation)
		
		TransformStack.of(ms)
			.rotateAround(rotation, 0.5f, 0.0f, 0.5f)
			.translate(localOffset)
			.rotateXDegrees(90f)
			.scale(scale, scale, scale)
	}
}
