@file:JvmName("TravelingPointSplitUtils")

package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.Create
import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.graph.*
import net.createmod.catnip.data.Couple
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min


open class TravellingPointForSplit(
	node1: TrackNode?,
	node2: TrackNode?,
	edge: TrackEdge?,
	position: Double,
	upsideDown: Boolean,
) : TravellingPoint(node1, node2, edge, position, upsideDown) {
	constructor() : this(node1 = null, node2 = null, edge = null, position = 0.0, upsideDown = false)
	
	constructor(from: TravellingPoint) : this(from.node1, from.node2, from.edge, from.position, from.upsideDown) {
		blocked = from.blocked
	}
	
	companion object {
		@JvmStatic
		fun read(tag: CompoundTag, graph: TrackGraph?, dimensions: DimensionPalette): TravellingPointForSplit {
			if(graph == null) return TravellingPointForSplit()
			
			val destination = if("railx:DestinationGraph" in tag) {
				RailwayServerGlobals.trackNetworks[tag.getUUID("railx:DestinationGraph")]
			} else null
			
			return TravellingPointForSplit(TravellingPoint.read(tag, destination ?: graph, dimensions))
				.also { it.destinationGraph = destination }
		}
	}
	
	
	var destinationGraph: TrackGraph? = null
	
	override fun travel(
		graph: TrackGraph,
		distance: Double,
		trackSelector: ITrackSelector,
		signalListener: IEdgePointListener,
		turnListener: ITurnListener,
		portalListener: IPortalListener,
	): Double {
		val moved = super.travel(
			destinationGraph ?: graph,
			distance,
			trackSelector,
			signalListener,
			turnListener,
			portalListener
		)
		if(!blocked) return moved
		return moved + tryTravelThroughSplit(
			distance - moved,
			trackSelector,
			signalListener,
			turnListener,
			portalListener
		)
	}
	
	private fun tryTravelThroughSplit(
		distance: Double,
		trackSelector: ITrackSelector,
		signalListener: IEdgePointListener,
		turnListener: ITurnListener,
		portalListener: IPortalListener,
	): Double {
		val split = when(position) {
			0.0 -> node1
			(edge ?: return 0.0).length -> node2
			else -> return 0.0
		}
		if(split !is SplittingTrackNode) return 0.0
		
		val otherGraph = Create.RAILWAYS.trackNetworks[split.otherGraph] ?: return 0.0
		val otherSplit = otherGraph.locateNode(split.location) ?: return 0.0
		val (otherEnd, otherEdge) = otherGraph.getConnectionsFrom(otherSplit).entries.singleOrNull() ?: return 0.0
		val otherLength = otherEdge.length
		
		val moved: Double
		if(distance >= 0) {
			node1 = otherSplit
			node2 = otherEnd
			edge = otherEdge
			moved = min(distance, otherLength)
			position = moved
		} else {
			node1 = otherEnd
			node2 = otherSplit
			edge = otherGraph.getConnection(Couple.create(otherEnd, otherSplit))!!
			moved = max(distance, -otherLength)
			position = otherLength + moved
		}
		blocked = false
		
		destinationGraph = otherGraph
		val remaining = distance - moved
		return if(remaining != 0.0) {
			val moved2 = travel(
				otherGraph, remaining,
				trackSelector, signalListener, turnListener, portalListener,
			)
			moved + moved2
		} else {
			moved // == distance
		}
	}
	
	
	override fun edgeTraversedFrom(
		graph: TrackGraph,
		forward: Boolean,
		edgePointListener: IEdgePointListener,
		turnListener: ITurnListener,
		prevPos: Double,
		totalDistance: Double,
	): Double? = super.edgeTraversedFrom(
		destinationGraph ?: graph,
		forward,
		edgePointListener, turnListener,
		prevPos,
		totalDistance,
	)
	
	override fun reverse(graph: TrackGraph) {
		super.reverse(destinationGraph ?: graph)
	}
	
	override fun getPositionWithOffset(trackGraph: TrackGraph?, offset: Double, flipUpsideDown: Boolean): Vec3 =
		super.getPositionWithOffset(destinationGraph ?: trackGraph, offset, flipUpsideDown)
	
	override fun migrateTo(locations: MutableList<TrackGraphLocation>) {
		val location = locations.removeAt(0)
		val graph = location.graph
		node1 = graph.locateNode(location.edge.first)
		node2 = graph.locateNode(location.edge.second)
		edge = graph.getConnectionsFrom(node1)[node2]
		position = location.position
		if(locations is MigrateTo) destinationGraph = graph
	}
	
	override fun write(dimensions: DimensionPalette): CompoundTag = super.write(dimensions).also { tag ->
		destinationGraph?.let { tag.putUUID("railx:DestinationGraph", it.id) }
	}
	
	
	class MigrateTo : ArrayList<TrackGraphLocation>()
}


class TravellingPointForSplitScout : TravellingPointForSplit() {
	override fun travel(
		graph: TrackGraph,
		distance: Double,
		trackSelector: ITrackSelector,
		signalListener: IEdgePointListener,
		turnListener: ITurnListener,
		portalListener: IPortalListener,
	): Double = try {
		super.travel(graph, distance, trackSelector, signalListener, turnListener, portalListener)
	} finally {
		blocked = false
		destinationGraph = null
	}
}
