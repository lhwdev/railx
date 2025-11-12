package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.content.trains.graph.TrackGraph


@Suppress("FunctionName")
interface SplittingNavigation {
	fun `railx$currentPathGraph`(): TrackGraph?
}
