package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.Create
import com.simibubi.create.content.trains.RailwaySavedData
import com.simibubi.create.content.trains.graph.TrackGraph
import java.util.*


object RailwayServerGlobals {
	var savedData: RailwaySavedData? = null
	
	
	val trackNetworks: Map<UUID, TrackGraph>
		get() = savedData?.trackNetworks ?: Create.RAILWAYS.trackNetworks
}
