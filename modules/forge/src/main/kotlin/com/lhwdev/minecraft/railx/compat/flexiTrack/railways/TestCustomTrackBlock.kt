package com.lhwdev.minecraft.railx.compat.flexiTrack.railways

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.railwayteam.railways.content.custom_tracks.NoCollisionCustomTrackBlock
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.content.trains.track.TrackBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape


class TestCustomTrackBlock(properties: Properties, material: FlexiTrackMaterial) :
	FlexiTrackBlock(properties, material) {
	override fun getCollisionShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext,
	): VoxelShape {
		(CRTrackMaterials.getBaseFromWide(material)?.block as? NoCollisionCustomTrackBlock)
			?.let { return it.getCollisionShape(state, level, pos, context) }
		return super.getCollisionShape(state, level, pos, context)
	}
}
