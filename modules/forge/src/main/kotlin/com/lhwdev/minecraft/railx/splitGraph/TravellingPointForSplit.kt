@file:JvmName("TravelingPointSplitUtils")

package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.Create
import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.entity.TravellingPoint.*
import com.simibubi.create.content.trains.graph.*
import net.createmod.catnip.data.Couple
import net.minecraft.nbt.CompoundTag
import kotlin.math.max
import kotlin.math.min


// implemented by TravellingPointMixin

/**
 * Main design consideration:
 * - should be flexible enough to cover most use cases
 * - does not require mixin to third party mods using [TravellingPoint]
 *
 * Should cover stateless + stateful cases; stateless case in `Navigation.signalScout` and stateful for others.
 * [MovingTravellingPoint] has more feature, including serialization and migration support.
 */
@Suppress("PropertyName", "FunctionName")
@JvmDefaultWithoutCompatibility // be sure to put this on implementing class
interface TravellingPointForSplit {
	var `railx$destinationGraph`: TrackGraph?
		get() = error("stub")
		set(_) = error("stub")
	
	fun `railx$prepareTravel`(graph: TrackGraph): TrackGraph = error("stub")
}

var TravellingPoint.destinationGraph: TrackGraph?
	get() = (this as TravellingPointForSplit).`railx$destinationGraph`
	set(value) {
		(this as TravellingPointForSplit).`railx$destinationGraph` = value
	}


object TravellingPointForSplitHelper {
	fun TravellingPoint.tryTravelThroughSplit(
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
	
}


@JvmDefaultWithoutCompatibility // without this, synthetic 'railx$destinationGraph' that calls TravellingPointForSplit is created
open class MovingTravellingPoint(
	node1: TrackNode?,
	node2: TrackNode?,
	edge: TrackEdge?,
	position: Double,
	upsideDown: Boolean,
) : TravellingPoint(node1, node2, edge, position, upsideDown), TravellingPointForSplit {
	
	constructor() : this(node1 = null, node2 = null, edge = null, position = 0.0, upsideDown = false)
	
	constructor(from: TravellingPoint) : this(from.node1, from.node2, from.edge, from.position, from.upsideDown) {
		blocked = from.blocked
	}
	
	
	override fun `railx$prepareTravel`(graph: TrackGraph): TrackGraph =
		destinationGraph ?: graph
	
	
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
	
	
	companion object {
		@JvmStatic
		fun read(tag: CompoundTag, graph: TrackGraph?, dimensions: DimensionPalette): MovingTravellingPoint {
			if(graph == null) return MovingTravellingPoint()
			
			val destination = if("railx:DestinationGraph" in tag) {
				RailwayServerGlobals.trackNetworks[tag.getUUID("railx:DestinationGraph")]
			} else null
			
			return MovingTravellingPoint(TravellingPoint.read(tag, destination ?: graph, dimensions))
				.also { it.destinationGraph = destination }
		}
	}
	
	
	class MigrateTo : ArrayList<TrackGraphLocation>()
}
