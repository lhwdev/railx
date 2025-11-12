package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.Create
import com.simibubi.create.content.trains.graph.*
import com.simibubi.create.content.trains.signal.SignalBoundary
import com.simibubi.create.content.trains.signal.SignalPropagator
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.createmod.catnip.data.Iterate

object SplitSignalPropagator {
	fun collectChainedSignals(graph: TrackGraph, signal: SignalBoundary, front: Boolean): SlotObjects.ChainedSignals {
		val map = SlotObjects.ChainedSignals()
		walkSignals(
			graph = graph,
			signal = signal,
			front = front,
			boundaryCallback = { g, node, boundary ->
				map.put(boundary.id, !boundary.isPrimary(node))
				if(g != graph) map.graphs[boundary.id] = g
				false
			},
			nonBoundaryCallback = { _, _ -> false },
			forCollection = true
		)
		return map
	}
	
	
	class Frontier(val graph: TrackGraph, val current: TrackNode, val previous: TrackNode?)
	
	fun walkSignals(
		graph: TrackGraph,
		signal: SignalBoundary,
		front: Boolean,
		boundaryCallback: (TrackGraph, TrackNode, SignalBoundary) -> Boolean,
		nonBoundaryCallback: (TrackGraph, EdgeData) -> Boolean,
		forCollection: Boolean,
	) {
		val edgeLocation = signal.edgeLocation
		val startNodes = edgeLocation.map { graph.locateNode(it) }
		val startEdges = startNodes.mapWithParams(
			{ l1, l2 -> graph.getConnectionsFrom(l1)[l2] },
			startNodes.swap()
		)
		
		val node1 = startNodes[front]
		val node2 = startNodes[!front]
		val startEdge = startEdges[front]
		val oppositeEdge = startEdges[!front]
		
		if(startEdge == null) return
		
		if(!forCollection) {
			SignalPropagator.notifyTrains(graph, startEdge, oppositeEdge)
			startEdge.edgeData.refreshIntersectingSignalGroups(graph)
			Create.RAILWAYS.sync.edgeDataChanged(graph, node1, node2, startEdge, oppositeEdge)
		}
		
		// Check for signal on the same edge
		val immediateBoundary = startEdge.edgeData.next(EdgePointType.SIGNAL, signal.getLocationOn(startEdge))
		if(immediateBoundary != null) {
			if(boundaryCallback(graph, node1, immediateBoundary))
				startEdge.edgeData.refreshIntersectingSignalGroups(graph)
			return
		}
		
		// Search for any connected signals
		val frontier = ArrayList<Frontier>()
		frontier.add(Frontier(graph, current = node2, previous = node1))
		walkSignals(frontier, boundaryCallback, nonBoundaryCallback, forCollection)
	}
	
	private fun walkSignals(
		frontier: MutableList<Frontier>,
		boundaryCallback: (TrackGraph, TrackNode, SignalBoundary) -> Boolean,
		nonBoundaryCallback: (TrackGraph, EdgeData) -> Boolean,
		forCollection: Boolean,
	) {
		val visited = ReferenceOpenHashSet<TrackEdge>()
		fun walkEdge(graph: TrackGraph, entry: Frontier, current: TrackNode, next: TrackNode, edge: TrackEdge) {
			if(next === entry.previous) return
			
			// already checked this edge
			if(!visited.add(edge)) return
			
			// chain signal: check if reachable
			if(forCollection && !entry.graph.getConnectionsFrom(entry.previous!!)[entry.current]!!.canTravelTo(edge))
				return
			
			val oppositeEdge = graph.getConnectionsFrom(next)[current]!!
			visited.add(oppositeEdge)
			
			for(flip in Iterate.falseAndTrue) {
				val currentEdge = if(flip) oppositeEdge else edge
				val signalData = currentEdge.edgeData
				
				// no boundary-update group of edge
				if(!signalData.hasSignalBoundaries()) {
					if(nonBoundaryCallback(graph, signalData)) {
						SignalPropagator.notifyTrains(graph, currentEdge)
						Create.RAILWAYS.sync.edgeDataChanged(graph, current, next, edge, oppositeEdge)
					}
					continue
				}
				
				// other/own boundary found
				val nextBoundary = signalData.next(EdgePointType.SIGNAL, 0.0) ?: continue
				if(boundaryCallback(graph, current, nextBoundary)) {
					SignalPropagator.notifyTrains(graph, edge, oppositeEdge)
					currentEdge.edgeData.refreshIntersectingSignalGroups(graph)
					Create.RAILWAYS.sync.edgeDataChanged(graph, current, next, edge, oppositeEdge)
				}
				return
			}
			
			frontier.add(Frontier(graph, current = next, previous = current))
		}
		
		while(!frontier.isEmpty()) {
			val entry = frontier.removeAt(0)
			val graph = entry.graph
			val current = entry.current
			
			for((next, edge) in graph.getConnectionsFrom(current))
				walkEdge(graph, entry, current, next, edge)
			
			if(current is SplittingTrackNode) {
				val otherGraph = Create.RAILWAYS.trackNetworks[current.otherGraph] ?: continue
				val otherCurrent = otherGraph.locateNode(current.location) ?: continue
				for((next, edge) in otherGraph.getConnectionsFrom(otherCurrent))
					walkEdge(otherGraph, entry, otherCurrent, next, edge)
			}
		}
	}
}
