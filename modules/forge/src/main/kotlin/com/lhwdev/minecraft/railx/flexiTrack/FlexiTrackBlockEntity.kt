package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.utils.addOneTimeListener
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.foundation.blockEntity.IMergeableBE
import com.tterrag.registrate.util.OneTimeEventReceiver
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.client.event.RenderFrameEvent


class FlexiTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	TrackBlockEntity(type, pos, state), TransformableBlockEntity, IMergeableBE {
	
	var state: FlexiState = if(state is FlexiBlockState.Update) {
		notifyUpdate()
		state.mapState(FlexiState.Base)
	} else {
		FlexiState.Base
	}
	
	val shape: FlexiShape
		get() = state.shape
	
	val block: FlexiTrackBlock
		get() = blockState.block
	
	override fun getBlockState(): FlexiBlockState =
		super.getBlockState() as FlexiBlockState
	
	private var voxelShapeCache: Pair<FlexiState, VoxelShape>? = null
	
	
	@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
	override fun setBlockState(blockState: BlockState) {
		super.setBlockState(blockState)
		
		if(blockState is FlexiBlockState.Update) {
			println("railx:update from $blockState")
			updateState(blockState.mapState(state))
		}
	}
	
	fun overlayShape(direction: FlexiDirection): FlexiState {
		val state = state
		val shape = state.shape
		if(direction in shape.axes) return state
		return state.copy(shape = shape.insert(direction))
	}
	
	fun updateState(newState: FlexiState) {
		if(state == newState) return
		state = newState
		notifyUpdate()
	}
	
	fun voxelShape(): VoxelShape = voxelShapeCache?.let { cache -> cache.second.takeIf { cache.first == state } }
		?: FlexiTrackVoxelShapes.of(state).also { voxelShapeCache = state to it }
	
	
	override fun write(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		super.write(tag, registries, clientPacket)
		tag.put("FlexiState", state.write())
	}
	
	override fun read(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		if(tag.contains("BoundLocation")) return
		super.read(tag, registries, clientPacket)
		
		state = FlexiState.read(tag.getCompound("FlexiState"))
	}
	
	override fun bind(boundDimension: ResourceKey<Level>, boundLocation: BlockPos) {
		throw IllegalStateException("cannot bind flexi track into portal")
	}
}
