package com.lhwdev.minecraft.railx.splitGraph

import com.lhwdev.minecraft.railx.splitGraph.SplitTrackPropagator.updateConnectedId
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphTrack
import com.simibubi.create.Create
import com.simibubi.create.api.event.TrackGraphMergeEvent
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNode
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.signal.SignalPropagator
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackPropagator
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.neoforge.common.NeoForge


object SplitTrackPropagator {
	fun onRailAdded(reader: LevelAccessor, pos: BlockPos, state: BlockState): Pair<TrackGraph, TrackGraph>? {
		val track = state.block as? SplitGraphTrack ?: return null
		val impl = SplitTrackPropagatorImpl(reader, pos, state, track)
		return impl.onRailAdded()
	}
	
	fun ensureSplitTrackNode(reader: LevelAccessor, pos: BlockPos, state: BlockState) {
		val track = state.block as? SplitGraphTrack ?: return
		val point = track.getPoint(reader, pos, state)
		val from = point.fromDiscovered
		val center = point.centerDiscovered
		val to = point.toDiscovered
		
		val manager = Create.RAILWAYS
		val fromGraph = manager.getGraph(reader, from) ?: return
		val toGraph = manager.getGraph(reader, to) ?: return
		
		for(centerGraph in manager.getGraphs(reader, center)) {
			if(centerGraph == fromGraph || centerGraph == toGraph) continue
			centerGraph.removeNode(reader, center)
			if(centerGraph.isEmpty) {
				manager.removeGraphAndGroup(centerGraph)
				manager.sync.graphRemoved(centerGraph)
			}
		}
		
		val centerInFrom = fromGraph.locateNode(center) as? SplittingTrackNode ?: SplittingTrackNode(
			otherGraph = toGraph.id,
			location = center,
			netId = TrackGraph.nextNodeId(),
			normal = point.normal
		).also { fromGraph.addCreatedNode(it) }
		
		if(fromGraph.getConnection(Couple.create(fromGraph.locateNode(from), centerInFrom)) == null)
			fromGraph.connectNodes(reader, from, center, null)
		
		val centerInTo = toGraph.locateNode(center) as? SplittingTrackNode ?: SplittingTrackNode(
			otherGraph = fromGraph.id,
			location = center,
			netId = TrackGraph.nextNodeId(),
			normal = point.normal
		).also { toGraph.addCreatedNode(it) }
		
		if(toGraph.getConnection(Couple.create(toGraph.locateNode(to), centerInTo)) == null)
			toGraph.connectNodes(reader, to, center, null)
		
		if(centerInFrom.otherGraph != toGraph.id) {
			centerInFrom.otherGraph = toGraph.id
			SplittingTrackNodeSync.nodeOtherGraphChanged(fromGraph, centerInFrom)
		}
		if(centerInTo.otherGraph != fromGraph.id) {
			centerInTo.otherGraph = fromGraph.id
			SplittingTrackNodeSync.nodeOtherGraphChanged(toGraph, centerInTo)
		}
		
		updateConnectedId(fromGraph, toGraph)
	}
	
	internal fun updateConnectedId(fromGraph: TrackGraph, toGraph: TrackGraph) {
		val manager = Create.RAILWAYS
		val fromConnectedId = fromGraph.connectedId
		if(fromConnectedId != null && fromConnectedId in manager.trackNetworks) {
			val toConnectedId = toGraph.connectedId
			when {
				fromConnectedId == toConnectedId -> {}
				toConnectedId != null && toConnectedId in manager.trackNetworks -> {
					val allConnected = fromGraph.allConnectedGraphsIncludingSelf(manager)
					var fromCount = 0
					for(connected in allConnected) {
						if(connected.connectedId == fromConnectedId) fromCount++
						else if(connected.connectedId == toConnectedId) fromCount--
					}
					val id = if(fromCount >= 0) fromGraph.connectedId else toGraph.connectedId
					val packet = TrackGraphConnectedId.packetFor(id)
					for(connected in allConnected) {
						if(connected.connectedId == id) continue
						connected.connectedId = id
						packet.graphs += connected.id
					}
				}
				
				else -> toGraph.updateConnectedIdForAllConnected(fromConnectedId)
			}
		} else {
			val toConnectedId = toGraph.connectedId
			if(toConnectedId != null && toConnectedId in manager.trackNetworks) {
				fromGraph.updateConnectedIdForAllConnected(toConnectedId)
			} else {
				// this also automatically update for toGraph and connected
				fromGraph.updateConnectedIdForAllConnected(fromGraph.id)
			}
		}
	}
}


private class SplitTrackPropagatorImpl(
	private val reader: LevelAccessor,
	private val pos: BlockPos,
	state: BlockState,
	track: SplitGraphTrack,
) {
	private val manager = Create.RAILWAYS
	private val frontier = ArrayDeque<FrontierEntry>()
	private val addedNodes = mutableListOf<TrackNode>()
	
	private val point = track.getPoint(reader, pos, state)
	private val from = point.fromDiscovered
	private val center = point.centerDiscovered
	private val to = point.toDiscovered
	
	fun onRailAdded(): Pair<TrackGraph, TrackGraph> {
		
		// Design: (arrow = edge)
		// fromGraph  adjacent track block 1 -> from -> center
		// toGraph                                      center -> to -> adjacent track block 2
		// whereas 'center' is SplittingTrackNode.
		// TODO: failsafe where fromGraph == toGraph
		
		// Add 'to' point;
		// 1. Remove all immediately reachable node locations
		val fromGraphs = findConnectedGraphsFromSplitEnd(from)
		val toGraphs = findConnectedGraphsFromSplitEnd(to)
		findConnectedGraphsFromSplitEnd(center) // remove center nodes(in case they exists)
			.removeEmptyGraphs()
		
		// 2. Remove empty graphs
		fromGraphs.removeEmptyGraphs()
		toGraphs.removeEmptyGraphs()
		
		// Changed order following for optimizing TrackGraphSync
		// 3. Merge groups
		// 4. add end nodes(from, to) to graph
		// 5. Connect all nodes
		val fromGraph = fromGraphs.resolveToSingleGraph()
		fromGraph.createNodeIfAbsent(from)
		fromGraph.connectAllNodes(from)
		
		val toGraph = toGraphs.resolveToSingleGraph()
		toGraph.createNodeIfAbsent(to)
		toGraph.connectAllNodes(to)
		
		// 6. add center node; connect from -> center (fromGraph) / center -> to (toGraph)
		SplittingTrackNode(
			otherGraph = toGraph.id,
			location = center,
			netId = TrackGraph.nextNodeId(),
			normal = point.normal,
		).let { fromGraph.addCreatedNode(it) }
		fromGraph.connectNodes(reader, from, center, null)
		
		SplittingTrackNode(
			otherGraph = fromGraph.id,
			location = center,
			netId = TrackGraph.nextNodeId(),
			normal = point.normal,
		).let { toGraph.addCreatedNode(it) }
		toGraph.connectNodes(reader, to, center, null)
		
		// 7. manage connectedId
		updateConnectedId(fromGraph, toGraph)
		
		manager.markTracksDirty()
		for(added in addedNodes)
			SignalPropagator.notifySignalsOfNewNode(toGraph, added)
		
		return fromGraph to toGraph
	}
	
	private fun findConnectedGraphsFromSplitEnd(location: DiscoveredLocation): MutableSet<TrackGraph> {
		val connected = mutableSetOf<TrackGraph>()
		
		for(graph in manager.getGraphs(reader, location)) {
			val node = graph.locateNode(location)
			graph.removeNode(reader, location)
			manager.sync.nodeRemoved(graph, node)
			connected += graph
		}
		
		return connected
	}
	
	private fun MutableSet<TrackGraph>.removeEmptyGraphs() {
		iterator().let { iterator ->
			while(iterator.hasNext()) {
				val graph = iterator.next()
				if(!graph.isEmpty || size == 1) continue
				val manager = Create.RAILWAYS
				manager.removeGraphAndGroup(graph)
				manager.sync.graphRemoved(graph)
				iterator.remove()
			}
		}
	}
	
	private fun MutableSet<TrackGraph>.resolveToSingleGraph(): TrackGraph = when(size) {
		0 -> TrackGraph().also { graph -> manager.putGraph(graph) }
		
		1 -> first()
		
		else -> {
			val target = maxBy { it.nodes.size }
			for(other in this) {
				if(other == target) continue
				NeoForge.EVENT_BUS.post(TrackGraphMergeEvent(other, target))
				other.transferAll(target)
				manager.removeGraphAndGroup(other)
				manager.sync.graphRemoved(other)
			}
			target
		}
	}
	
	private fun TrackGraph.connectAllNodes(start: DiscoveredLocation) {
		frontier.clear()
		frontier.add(FrontierEntry(start, null, start))
		var emergencyExit = 1000
		while(frontier.isNotEmpty()) {
			if(emergencyExit-- == 0) break
			
			val entry = frontier.removeFirst()
			var parent = entry.parent
			val current = entry.current
			val ends = ITrackBlock.walkConnectedTracks(reader, current, false).toMutableList()
			entry.previous?.let { ends -= it }
			
			if(
				(entry.previous == null ||
					TrackPropagator.isValidGraphNodeLocation(current, ends, false)
					) && current != start
			) {
				val nodeIsNew = createNodeIfAbsent(current)
				val currentNode = locateNode(current)
				if(getConnection(Couple.create(locateNode(parent), currentNode)) == null)
					connectNodes(reader, parent, current, current.turn)
				addedNodes += currentNode
				parent = current
				if(!nodeIsNew) continue
			}
			continueSearchWithParent(entry, parent, ends)
		}
	}
	
	
	private class FrontierEntry(
		val parent: DiscoveredLocation,
		val previous: DiscoveredLocation?,
		val current: DiscoveredLocation,
	)
	
	private fun continueSearchWithParent(
		entry: FrontierEntry,
		parent: DiscoveredLocation,
		ends: Collection<DiscoveredLocation>,
	) {
		for(location in ends)
			frontier += FrontierEntry(parent, entry.current, location)
	}
}
