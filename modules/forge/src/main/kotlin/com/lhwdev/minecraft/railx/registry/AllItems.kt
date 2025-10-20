package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.buildTrack.TrackPlanItem
import com.tterrag.registrate.util.entry.ItemEntry

object AllItems {
	val Registry = RailXRegistry
	
	fun register() {}
	
	
	val TrackPlan: ItemEntry<TrackPlanItem> = Registry.item("track_plan", ::TrackPlanItem)
		.model { c, p -> p.withExistingParent(c.name, p.mcLoc("item/filled_map")) }
		.lang("Track Plan")
		.register()
}
