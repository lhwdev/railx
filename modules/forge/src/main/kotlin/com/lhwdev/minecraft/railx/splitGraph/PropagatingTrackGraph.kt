package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.content.trains.GlobalRailwayManager
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNode


class PropagatingTrackGraph(private val manager: GlobalRailwayManager, override var base: TrackGraph) :
	MergedTrackGraph(base.id) {
	override val graphs = mutableSetOf(base)
	
	override fun getConnectionsFrom(node: TrackNode?): Map<TrackNode, TrackEdge> {
		if(node is SplittingTrackNode) {
			val otherGraph = manager.trackNetworks[node.otherGraph]
			if(otherGraph != null) graphs += otherGraph
		}
		return super.getConnectionsFrom(node)
	}
}
