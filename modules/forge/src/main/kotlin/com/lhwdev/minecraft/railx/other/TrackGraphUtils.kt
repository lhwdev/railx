package com.lhwdev.minecraft.railx.other

import com.lhwdev.minecraft.railx.utils.dimension
import com.simibubi.create.Create
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Couple
import net.minecraft.world.level.LevelAccessor


class TrackEdgeInGraph(val graph: TrackGraph, val edge: TrackEdge) {
	operator fun component1(): TrackGraph = graph
	operator fun component2(): TrackEdge = edge
}


fun BezierConnection.toTrackEdge(level: LevelAccessor): TrackEdgeInGraph? {
	val from = TrackNodeLocation(starts.first).`in`(level.dimension())
	val to = TrackNodeLocation(starts.second).`in`(level.dimension())
	smoothing?.let {
		from.yOffsetPixels = it.first
		to.yOffsetPixels = it.second
	}
	
	for(graph in Create.RAILWAYS.sided(level).getGraphs(level, from)) {
		val fromNode = graph.locateNode(from) ?: continue
		val toNode = graph.locateNode(to) ?: continue
		return graph.getConnection(Couple.create(fromNode, toNode))
			?.let { TrackEdgeInGraph(graph, it) }
	}
	return null
}
