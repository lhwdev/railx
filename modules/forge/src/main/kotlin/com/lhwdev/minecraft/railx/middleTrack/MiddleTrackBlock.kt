package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.simibubi.create.content.trains.track.FakeTrackBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape


class MiddleTrackBlock(properties: Properties) : FakeTrackBlock(properties) {
	override fun getShape(
		pState: BlockState,
		pLevel: BlockGetter,
		pPos: BlockPos,
		pContext: CollisionContext,
	): VoxelShape {
		return box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
	}
	
	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
		AllBlockEntityTypes.MiddleTrack.create(pos, state)
}
