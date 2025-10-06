package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.blockEntity.IMergeableBE
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.VoxelShape


class FlexiTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	TrackBlockEntity(type, pos, state), TransformableBlockEntity, IMergeableBE {
	
	init {
		tilt = FlexiTrackBlockEntityTilt(this)
	}
	
	var state: FlexiState = if(state is FlexiBlockState.Update) {
		notifyUpdate()
		state.mapState(FlexiState.Base)
	} else {
		FlexiState.Base
	}
	
	val baseShape: FlexiShape
		get() = state.baseShape
	
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
		if(shape != state.baseShape) return state
		if(direction in shape.axes) return state
		return state.copy(baseShape = shape.insert(direction))
	}
	
	fun updateState(newState: FlexiState) {
		if(state == newState) return
		state = newState
		notifyUpdate()
	}
	
	fun updateEachConnections(updateConnection: (connection: BezierConnection) -> Unit) {
		val level = level!!
		val validConnections = connections.values.filter { connection ->
			val other = level.getBlockEntity(connection.key) as? TrackBlockEntity ?: return@filter false
			blockPos in other.connections
		}
		
		removeInboundConnections(false)
		TrackPropagator.onRailRemoved(level, blockPos, blockState)
		connections.clear()
		
		for(connection in validConnections) {
			updateConnection(connection)
			addConnection(connection)
			
			val otherPos = connection.key
			val otherState = level.getBlockState(otherPos)
			if(otherState.block !is ITrackBlock) continue
			level.setBlockAndUpdate(otherPos, otherState.setValue(TrackBlock.HAS_BE, true))
			val otherBe = level.getBlockEntity(otherPos)
			if(otherBe is TrackBlockEntity) {
				otherBe.addConnection(connection.secondary())
			}
		}
		
		notifyUpdate()
	}
	
	fun voxelShape(): VoxelShape = voxelShapeCache?.let { cache -> cache.second.takeIf { cache.first == state } }
		?: FlexiTrackVoxelShapes.of(state).also { voxelShapeCache = state to it }
	
	
	override fun removeConnection(target: BlockPos) {
		if(state.tilt != null) tilt.captureSmoothingHandles()
		super.removeConnection(target)
	}
	
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
