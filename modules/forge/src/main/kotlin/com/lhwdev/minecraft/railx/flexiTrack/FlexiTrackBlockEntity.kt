package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity
import com.simibubi.create.content.trains.track.TrackBlockEntity
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
	
	internal var shape: FlexiShape = FlexiShape.Empty
	
	private val state: FlexiBlockState
		get() = blockState as FlexiBlockState
	
	private var voxelShapeCache: VoxelShape? = null
	
	
	fun voxelShape(): VoxelShape = voxelShapeCache ?: run {
		FlexiTrackVoxelShapes.of(shape)
	}.also { voxelShapeCache = it }
	
	// override fun getModelData(): ModelData = ModelData.builder()
	// 	.also { if(isTilted) it.with(TrackBlockEntityTilt.ASCENDING_PROPERTY, tilt.smoothingAngle.get()) }
	// 	.with(FlexiTrackModel.ShapeProperty, shape)
	// 	.build()
	
	
	override fun write(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		super.write(tag, registries, clientPacket)
		tag.put("FlexiShape", shape.write())
	}
	
	override fun read(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		if(tag.contains("BoundLocation")) return
		super.read(tag, registries, clientPacket)
		
		shape = FlexiShape.read(tag.getCompound("FlexiState"))
		level?.setBlockSilently(blockPos, state.setShape(shape))
	}
	
	override fun bind(boundDimension: ResourceKey<Level>, boundLocation: BlockPos) {
		throw IllegalStateException("cannot bind flexi track into portal")
	}
}

// Note: maybe somewhat fragile; doesn't need to call any listener / do not update anything other than shape
private fun Level.setBlockSilently(pos: BlockPos, state: BlockState) {
	val chunk = getChunkAt(pos)
	val section = chunk.getSection(chunk.getSectionIndex(pos.y))
	
	val j = pos.x and 15
	val k: Int = pos.y and 15
	val l = pos.z and 15
	section.setBlockState(j, k, l, state)
}
