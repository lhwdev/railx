package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiShape
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockBehavior
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.other.ScrollValueBehaviorExtension
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
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
import kotlin.math.PI


class FlexiTrackRotateScrollBehaviors(be: FlexiTrackBlockEntity) :
	ScrollValueBehaviour(Component.literal("Rotate Flexi Track"), be, FlexiRotationValueBox()),
	FlexiTrackBlockBehavior, ScrollValueBehaviorExtension {
	
	enum class Kind {
		Direction,
		Gradient,
		Tilt;
		
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
}

private class FlexiRotationValueBox : ValueBoxTransform.Sided() {
	override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 =
		Vec3(0.5, 0.25, 0.5)
	
	override fun getSouthLocation(): Vec3 = Vec3.ZERO
	
	override fun isSideActive(state: BlockState, direction: Direction): Boolean =
		direction == Direction.UP
	
	override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
		val blockEntity = level.getBlockEntity(pos) as? FlexiTrackBlockEntity ?: return
		val direction = blockEntity.state.shape.axis1
		val directionCache = blockEntity.state.shapeCache.firstOrNull() ?: return
		val player = Minecraft.getInstance().player ?: return
		val sign = player.lookAngle.dot(direction.tangent)
		
		val halfPi = PI.toFloat() * 0.5f
		TransformStack.of(ms)
			.rotate(directionCache.rotationValue)
			.rotateY(if(sign < 0) halfPi else halfPi * 3)
		
		super.rotate(level, pos, state, ms)
	}
}
