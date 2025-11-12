package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.content.trains.GlobalRailwayManager
import com.simibubi.create.content.trains.graph.*
import com.simibubi.create.content.trains.signal.TrackEdgePoint
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Couple
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.collections.AbstractCollection
import kotlin.collections.AbstractSet


abstract class MergedTrackGraphBase(graphId: UUID) : TrackGraph(graphId), TrackGraphForSplit {
	companion object {
		@JvmStatic
		fun getBase(from: TrackGraph): TrackGraph = if(from is MergedTrackGraphBase) from.base else from
	}
	
	abstract val base: TrackGraph
	
	abstract val graphs: Iterable<TrackGraph>
	
	
	override fun `railx$getConnectedId`(): UUID? =
		base.connectedId
	
	override fun `railx$setConnectedId`(id: UUID?) {
		graphs.forEach {
			it.connectedId = id
			TrackGraphConnectedId.connectedIdChanged(it)
		}
	}
	
	override fun `railx$addCreatedNode`(node: TrackNode) {
		(base as TrackGraphForSplit).`railx$addCreatedNode`(node)
	}
	
	override fun `railx$loadSplittingNode`(
		data: SplittingTrackNode.Data,
		location: TrackNodeLocation,
		netId: Int,
		normal: Vec3,
	) {
		(base as TrackGraphForSplit).`railx$loadSplittingNode`(data, location, netId, normal)
	}
	
	override fun `railx$connectedGraphs`(): Collection<UUID> = mutableSetOf<UUID>().also { set ->
		graphs.flatMapTo(set) {
			set += it.id
			it.connectedGraphs
		}
	}
	
	override fun `railx$allConnectedGraphsIncludingSelf`(manager: GlobalRailwayManager): Set<TrackGraph> =
		base.allConnectedGraphsIncludingSelf(manager)
	
	override fun <T : TrackEdgePoint> addPoint(type: EdgePointType<T>, point: T) {
		base.addPoint(type, point)
	}
	
	override fun <T : TrackEdgePoint> getPoint(type: EdgePointType<T>, id: UUID?): T? =
		graphs.firstNotNullOfOrNull { it.getPoint(type, id) }
	
	override fun <T : TrackEdgePoint> getPoints(type: EdgePointType<T>): Collection<T> =
		object : AbstractCollection<T>() {
			override fun iterator(): Iterator<T> = object : Iterator<T> {
				private var index = 0
				private val list = graphs.toList()
				private var it = list[0].getPoints(type).iterator()
				
				private fun resolve() {
					while(index != -1 && !it.hasNext()) {
						val i = ++index
						if(i >= list.size) index = -1
						else it = list[i].getPoints(type).iterator()
					}
				}
				
				init {
					resolve()
				}
				
				override fun hasNext(): Boolean = index != -1
				
				override fun next(): T =
					it.next().also { resolve() }
			}
			
			override fun contains(element: T): Boolean =
				graphs.any { it.getPoints(type).contains(element) }
			
			override val size: Int
				get() = 1 + graphs.sumOf { it.getPoints(type).size }
		}
	
	override fun <T : TrackEdgePoint> removePoint(type: EdgePointType<T>, id: UUID): T? =
		graphs.firstNotNullOfOrNull { it.removePoint(type, id) }
	
	override fun getBounds(level: Level): TrackGraphBounds = base.getBounds(level)
	
	override fun getNodes(): Set<TrackNodeLocation> = object : AbstractSet<TrackNodeLocation>() {
		override fun iterator(): Iterator<TrackNodeLocation> = object : Iterator<TrackNodeLocation> {
			private var index = 0
			private val list = graphs.toList()
			private var it = list[0].nodes.iterator()
			
			private fun resolve() {
				while(index != -1 && !it.hasNext()) {
					val i = ++index
					if(i >= list.size) index = -1
					else it = list[i].nodes.iterator()
				}
			}
			
			init {
				resolve()
			}
			
			override fun hasNext(): Boolean = index != -1
			
			override fun next(): TrackNodeLocation =
				it.next().also { resolve() }
		}
		
		override fun contains(element: TrackNodeLocation): Boolean =
			graphs.any { it.locateNode(element) != null }
		
		override val size: Int
			get() = graphs.sumOf { it.nodes.size }
	}
	
	override fun locateNode(level: Level, position: Vec3): TrackNode? =
		locateNode(TrackNodeLocation(position).`in`(level))
	
	override fun locateNode(position: TrackNodeLocation): TrackNode? =
		graphs.firstNotNullOfOrNull { it.locateNode(position) }
	
	override fun getNode(netId: Int): TrackNode? =
		graphs.firstNotNullOfOrNull { it.getNode(netId) }
	
	override fun createNodeIfAbsent(location: TrackNodeLocation.DiscoveredLocation): Boolean {
		if(locateNode(location) != null) return false
		return base.createNodeIfAbsent(location)
	}
	
	override fun addNode(node: TrackNode) {
		base.addNode(node)
	}
	
	override fun addNodeIfAbsent(node: TrackNode): Boolean {
		if(locateNode(node.location) != null) return false
		return base.addNodeIfAbsent(node)
	}
	
	override fun removeNode(level: LevelAccessor?, location: TrackNodeLocation): Boolean =
		graphs.any { it.removeNode(level, location) }
	
	override fun transferAll(toOther: TrackGraph) {
		graphs.forEach { it.transferAll(toOther) }
	}
	
	override fun getChecksum(): Int = base.getChecksum()
	
	override fun isEmpty(): Boolean = base.isEmpty
	
	override fun getConnectionsFrom(node: TrackNode?): Map<TrackNode, TrackEdge> {
		if(node == null) return emptyMap()
		
		class ConnectionsFrom(private val graph: TrackGraph, from: Map<TrackNode, TrackEdge>) :
			IdentityHashMap<TrackNode, TrackEdge>(from) {
			override fun containsKey(key: TrackNode): Boolean {
				if(super.containsKey(key)) return true
				if(key !is SplittingTrackNode) return false
				return super.containsKey(graph.locateNode(key.location))
			}
			
			override fun get(key: TrackNode): TrackEdge? {
				super.get(key)?.let { return it }
				if(key !is SplittingTrackNode) return null
				return super.get(graph.locateNode(key.location))
			}
			
			override fun getOrDefault(key: TrackNode, defaultValue: TrackEdge): TrackEdge =
				get(key) ?: defaultValue
		}
		
		if(node is SplittingTrackNode) {
			// TrackNode is unique as identity in one graph
			val result = HashMap<TrackNode, TrackEdge>()
			for(graph in graphs) {
				val otherNode = graph.locateNode(node.location) ?: continue
				result += graph.getConnectionsFrom(otherNode)
			}
			return result
		}
		
		for(graph in graphs) {
			val result = graph.getConnectionsFrom(node)
			if(result.isEmpty()) continue
			if(result is ConnectionsFrom) return result
			return if(result.keys.any { it is SplittingTrackNode }) {
				ConnectionsFrom(graph, result)
			} else {
				result
			}
		}
		return emptyMap()
	}
	
	override fun getConnection(nodes: Couple<TrackNode>): TrackEdge? =
		getConnectionsFrom(nodes.first).get(nodes.second)
	
	override fun connectNodes(
		reader: LevelAccessor?,
		location: TrackNodeLocation.DiscoveredLocation,
		location2: TrackNodeLocation.DiscoveredLocation,
		turn: BezierConnection?,
	) {
		base.connectNodes(reader, location, location2, turn)
	}
	
	override fun disconnectNodes(node1: TrackNode, node2: TrackNode) {
		base.disconnectNodes(node1, node2)
	}
	
	override fun putConnection(node1: TrackNode, node2: TrackNode, edge: TrackEdge): Boolean =
		base.putConnection(node1, node2, edge)
}


abstract class MergedTrackGraph(graphId: UUID) : MergedTrackGraphBase(graphId) {
	companion object {
		@JvmStatic
		fun contains(graph: MergedTrackGraph, other: TrackGraph): Boolean =
			getBase(other) in graph.graphs
	}
	
	override val base: TrackGraph
		get() = graphs.first()
	abstract override val graphs: Collection<TrackGraph>
}

class MergedTrackGraphImpl(override val graphs: Collection<TrackGraph>) :
	MergedTrackGraph(graphId = graphs.first().id)

class MutableMergedTrackGraph(override val graphs: MutableList<TrackGraph>) :
	MergedTrackGraph(graphId = graphs.first().id)

class AllConnectedTrackGraphs(manager: GlobalRailwayManager, override val base: TrackGraph) :
	MergedTrackGraph(graphId = base.id) {
	override val graphs: Set<TrackGraph> = base.allConnectedGraphsIncludingSelf(manager)
	
	override fun `railx$connectedGraphs`(): Collection<UUID> =
		graphs.map { it.id }
	
	override fun `railx$allConnectedGraphsIncludingSelf`(manager: GlobalRailwayManager): Set<TrackGraph> = graphs
}

class ConnectedTrackGraphs(private val manager: GlobalRailwayManager, override val base: TrackGraph) :
	MergedTrackGraph(graphId = base.id) {
	override val graphs: List<TrackGraph> = buildList {
		add(base)
		if(base is MergedTrackGraph) {
			base.connectedGraphs.mapNotNullTo(this) {
				manager.trackNetworks[it].takeIf { graph -> graph !in base.graphs }
			}
		} else {
			base.connectedGraphs.mapNotNullTo(this) { manager.trackNetworks[it] }
		}
	}
	
	override fun `railx$connectedGraphs`(): Collection<UUID> =
		graphs.map { it.id }
}


// Special TrackGraphs

internal class PropagatingTrackGraphForNavigationRead(
	private val networks: Map<UUID, TrackGraph>,
	override val base: TrackGraph,
) : MergedTrackGraph(graphId = base.id) {
	private val ids = hashSetOf<UUID>()
	override val graphs = mutableListOf<TrackGraph>()
	
	override fun locateNode(position: TrackNodeLocation): TrackNode? = super.locateNode(position)?.also { node ->
		if(node is SplittingTrackNode) {
			if(ids.add(node.otherGraph)) {
				val otherGraph = networks[node.otherGraph]
				if(otherGraph != null) graphs += otherGraph
			}
		}
	}
}
