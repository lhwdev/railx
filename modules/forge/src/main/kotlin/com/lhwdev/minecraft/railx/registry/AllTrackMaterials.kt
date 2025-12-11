package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.simibubi.create.Create
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackMaterialFactory


object AllTrackMaterials {
	@Suppress("UnusedExpression")
	fun register() {
		SplitGraph
		FlexiSplitGraph
	}
	
	object SplitGraph {
		val Andesite: TrackMaterial = TrackMaterialFactory.make(RailX.asResource("split_graph/andesite")).apply {
			lang("Split Graph Andesite")
			block { AllBlocks.SplitGraphTrack }
			particle(Create.asResource("block/palettes/stone_types/polished/andesite_cut_polished"))
			defaultModels()
		}.build()
	}
	
	object FlexiSplitGraph {
		val Andesite: FlexiTrackMaterial = FlexiTrackMaterial(RailX.asResource("flexi_split_graph/andesite")) {
			langName = "Flexible Split Graph Andesite"
			trackBlock = AllBlocks.FlexiSplitGraphTrack
			normalBlock = AllBlocks.SplitGraphTrack
			particle = Create.asResource("block/palettes/stone_types/polished/andesite_cut_polished")
			defaultModels()
		}
	}
}
