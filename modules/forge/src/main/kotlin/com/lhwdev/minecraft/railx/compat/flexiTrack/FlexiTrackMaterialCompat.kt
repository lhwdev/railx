package com.lhwdev.minecraft.railx.compat.flexiTrack

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.railwayteam.railways.registry.CRTrackMaterials


object FlexiTrackMaterialCompat {
	val WideDarkOak = FlexiTrackMaterial(id = RailX.asResource("flexi_wide_dark_oak")) {
		val base = CRTrackMaterials.WIDE_GAUGE[CRTrackMaterials.DARK_OAK]!!
		
		langName = base.langName
		trackBlock { AllBlocks.WideDarkOakFlexiTrack!! }
		normalTrackBlock { { base.block } }
		trackType = base.trackType
		particle = base.particle // TODO
		noRecipeGen()
		standardModels(id = base.id)
	}
}
