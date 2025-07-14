package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.content.trains.track.TrackBlockEntityTilt
import com.simibubi.create.foundation.blockEntity.IMergeableBE
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.client.model.data.ModelData


class FlexiTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	TrackBlockEntity(type, pos, state), TransformableBlockEntity, IMergeableBE {
	
	var shape: FlexiShape = FlexiShape.Single(FlexiDirection.Known.Divisions[0])
		set(value) {
			field = value
			voxelShapeCache = null
			notifyUpdate()
		}
	
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
	}
	
	override fun bind(boundDimension: ResourceKey<Level>, boundLocation: BlockPos) {
		throw IllegalStateException("cannot bind flexi track into portal")
	}
}
