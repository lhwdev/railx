package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVoxelShapes
import com.simibubi.create.content.trains.track.TrackMaterial


class MonorailFlexiTrackBlock(properties: Properties, material: TrackMaterial) : FlexiTrackBlock(properties, material) {
	override val voxelShapes: FlexiTrackVoxelShapes
		get() = RailwaysFlexiTrackVoxelShapes.Monorail
}
