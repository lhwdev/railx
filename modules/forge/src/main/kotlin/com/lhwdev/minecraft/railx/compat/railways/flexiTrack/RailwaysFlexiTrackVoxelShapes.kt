package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVoxelShapes
import com.railwayteam.railways.registry.CRShapes
import net.minecraft.core.Direction


object RailwaysFlexiTrackVoxelShapes {
	val NarrowGauge: FlexiTrackVoxelShapes =
		FlexiTrackVoxelShapes.fromSingleBoxShape(CRShapes.NARROW_TRACK_ORTHO[Direction.EAST])
	
	val Monorail: FlexiTrackVoxelShapes =
		FlexiTrackVoxelShapes.fromSingleBoxShape(CRShapes.MONORAIL_TRACK_ORTHO[Direction.EAST])
}
