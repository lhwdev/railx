package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.simibubi.create.content.trains.track.FakeTrackBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState


class MiddleTrackBlock(properties: Properties) : FakeTrackBlock(properties) {
	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
		AllBlockEntityTypes.MiddleTrack.create(pos, state)
}