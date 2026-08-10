package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.common.ScrollValueBehaviorExtension
import com.lhwdev.minecraft.railx.common.ValueSettingsBehaviourExtra
import com.lhwdev.minecraft.railx.flexiTrack.*
import com.lhwdev.minecraft.utils.vectors.toVec3
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
import net.minecraft.core.HolderLookup
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
import org.joml.Vector3d
import java.util.function.Consumer
import kotlin.math.PI


class FlexiTrackRotateScrollBehaviors(be: FlexiTrackBlockEntity) :
	ScrollValueBehaviour(Component.literal("Rotate Flexi Track"), be, FlexiRotationValueBox),
	FlexiTrackBlockBehavior, ValueSettingsBehaviourExtra, ScrollValueBehaviorExtension {
	
	enum class Kind(val title: String) {
		Direction(title = "Direction"),
		Gradient(title = "Gradient"),
		Cant(title = "Cant");
		
		fun next(): Kind = entries[(ordinal + 1) % entries.size]
	}
	
	companion object {
		var currentKind: Kind = Kind.Direction
		
		const val ClipboardKey = "railx:FlexiTrackRotation"
		
		fun writeToClipboard(axis: FlexiDirection, tag: CompoundTag) {
			tag.put("Direction", axis.write())
		}
		
		fun readFromClipboard(tag: CompoundTag): FlexiDirection? =
			(tag.get("Direction") as? CompoundTag)
				?.let { FlexiDirection.read(it) }
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
		Kind.Cant -> FlexiCantScrollBehavior(be, slotPositioning)
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
			.append(kindText(Kind.Cant))
			.append(".")
	}
	
	override fun read(nbt: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {}
	override fun write(nbt: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {}
	
	override fun writeToClipboard(registries: HolderLookup.Provider, tag: CompoundTag, side: Direction): Boolean {
		val axis = be.shape.axes.singleOrNull() ?: return false
		writeToClipboard(axis, tag)
		return true
	}
	
	override fun readFromClipboard(
		registries: HolderLookup.Provider,
		tag: CompoundTag,
		player: Player,
		side: Direction,
		simulate: Boolean,
	): Boolean {
		val state = be.state
		if(state.shape.axes.size != 1) return false
		val direction = readFromClipboard(tag) ?: return false
		if(simulate) return true
		
		be.updateState(state.copy(baseShape = FlexiShape.Single(axis = direction)))
		return true
	}
	
	override fun getClipboardKey(): String =
		ClipboardKey
}

private object FlexiRotationValueBox : ValueBoxTransform.Sided() {
	override fun getSouthLocation(): Vec3? = null // will directly implement getLocalOffset()
	
	override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3? {
		val block = state.block as? FlexiTrackBlock ?: return null
		val blockEntity = level.getBlockEntity(pos) as? FlexiTrackBlockEntity ?: return null
		
		val height = block.voxelShapes.base.let { it.max(Direction.Axis.Y) - it.min(Direction.Axis.Y) }
		val shape = blockEntity.state.shapeCache.firstOrNull() ?: return null
		
		return Vector3d(0.5, height, 0.5)
			.sub(blockEntity.center)
			.rotate(shape.rotationValueDouble)
			.add(blockEntity.center)
			.toVec3()
	}
	
	override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
		val block = state.block as? FlexiTrackBlock ?: return
		val blockEntity = level.getBlockEntity(pos) as? FlexiTrackBlockEntity ?: return
		val player = Minecraft.getInstance().player ?: return
		
		val shape = blockEntity.state.shapeCache.firstOrNull() ?: return
		val sign = player.lookAngle.dot(shape.direction.tangent)
		
		val halfPi = PI.toFloat() * 0.5f
		TransformStack.of(ms)
			.rotate(shape.rotationValue)
			.rotateX(halfPi)
			.rotateZ(if(sign < 0) halfPi else halfPi * 3)
	}
	
	override fun isSideActive(state: BlockState, direction: Direction): Boolean =
		direction == Direction.UP
}
