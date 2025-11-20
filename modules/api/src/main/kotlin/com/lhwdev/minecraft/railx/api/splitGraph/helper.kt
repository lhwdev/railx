@file:JvmName("SplitGraphHelper")

package com.lhwdev.minecraft.railx.api.splitGraph

import com.lhwdev.minecraft.railx.api.apiOrNull
import com.lhwdev.minecraft.railx.api.isApiLoaded
import com.lhwdev.minecraft.railx.splitGraph.MergedTrackGraph
import com.lhwdev.minecraft.railx.splitGraph.MergedTrackGraphImpl
import com.lhwdev.minecraft.railx.splitGraph.MovingTravellingPoint
import com.lhwdev.minecraft.railx.splitGraph.TrackGraphConnectedId
import com.lhwdev.minecraft.railx.splitGraph.allConnectedGraphsIncludingSelf
import com.lhwdev.minecraft.railx.splitGraph.connectedGraphs
import com.lhwdev.minecraft.railx.splitGraph.connectedId
import com.lhwdev.minecraft.railx.splitGraph.destinationGraph
import com.lhwdev.minecraft.railx.splitGraph.replaceGraphPreserving
import com.simibubi.create.content.trains.GlobalRailwayManager
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.graph.DimensionPalette
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNode
import net.minecraft.nbt.CompoundTag
import java.util.*


/**
 * Be aware that this also invokes client sync by `TrackGraphConnectedId.connectedIdChanged()`. If you don't want this
 * behavior, use mod jar itself.
 */
var TrackGraph.connectedId: UUID?
	get() = apiOrNull { connectedId }
	set(value) {
		if(!isApiLoaded) return
		connectedId = value
		TrackGraphConnectedId.connectedIdChanged(this)
	}

val TrackGraph.connectedGraphs: Collection<UUID>
	get() = apiOrNull { connectedGraphs } ?: emptyList()

fun TrackGraph.allConnectedGraphsIncludingSelf(manager: GlobalRailwayManager): Set<TrackGraph> =
	apiOrNull { allConnectedGraphsIncludingSelf(manager) } ?: emptySet()


fun mergedTrackGraphOf(graphs: Collection<TrackGraph>): TrackGraph? =
	apiOrNull { MergedTrackGraphImpl(graphs) }

val TrackGraph.mergedGraphsOrNull: Collection<TrackGraph>?
	get() = apiOrNull { (this as? MergedTrackGraph)?.graphs }

val TrackGraph.mergedGraphs: Collection<TrackGraph>
	get() = mergedGraphsOrNull ?: listOf(this)


var TravellingPoint.destinationGraph: TrackGraph?
	get() = apiOrNull { destinationGraph }
	set(value) {
		if(isApiLoaded) destinationGraph = value
	}


fun Train.replaceGraphPreserving(from: TrackGraph?, to: TrackGraph?) {
	if(isApiLoaded) replaceGraphPreserving(from, to)
	else if(graph == from) graph = to
}


object SplittingTrackNodeUtils {

}


object MovingTravellingPointUtils {
	@JvmStatic
	fun create(): TravellingPoint =
		apiOrNull { MovingTravellingPoint() } ?: TravellingPoint()
	
	@JvmStatic
	fun create(
		node1: TrackNode?,
		node2: TrackNode?,
		edge: TrackEdge?,
		position: Double,
		upsideDown: Boolean,
	): TravellingPoint =
		apiOrNull { MovingTravellingPoint(node1, node2, edge, position, upsideDown) } ?: TravellingPoint()
	
	@JvmStatic
	fun isInstance(point: TravellingPoint): Boolean =
		apiOrNull { point is MovingTravellingPoint } ?: false
	
	@JvmStatic
	fun read(tag: CompoundTag, graph: TrackGraph?, dimensions: DimensionPalette): TravellingPoint =
		apiOrNull { MovingTravellingPoint.read(tag, graph, dimensions) }
			?: TravellingPoint.read(tag, graph, dimensions)
}
