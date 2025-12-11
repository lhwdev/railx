package com.lhwdev.minecraft.railx.compat.flexiTrack

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.railwayteam.railways.registry.CRBlocks
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.Create


object FlexiTrackMaterialCompat {
	val WideAndesite = FlexiTrackMaterial(id = RailX.asResource("flexi_wide_andesite")) {
		langName = "Andesite"
		trackBlock { AllBlocks.WideAndesiteFlexiTrack }
		normalTrackBlock = CRBlocks.WIDE_GAUGE_TRACKS[CRTrackMaterials.WIDE_GAUGE_ANDESITE]!!
		trackType = CRTrackMaterials.CRTrackType.WIDE_GAUGE
		particle = Create.asResource("block/palettes/stone_types/polished/andesite_cut_polished")
		noRecipeGen()
		defaultModels()
	}
}
