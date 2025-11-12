package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.graph.*
import com.simibubi.create.content.trains.station.GlobalStation
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Pair
import net.minecraft.world.phys.Vec3
import java.util.*


object SlotObjects {
	class StationPredicate(val condition: (GlobalStation) -> Boolean) : ArrayList<GlobalStation>()
	
	class ChainedSignals : Object2BooleanOpenHashMap<UUID>() {
		@JvmField
		val graphs: HashMap<UUID, TrackGraph> = HashMap()
	}
	
	class TrackGraphSyncPacketSplitNodePair(
		val data: SplittingTrackNode.Data,
		location: TrackNodeLocation,
		normal: Vec3,
	) :
		Pair<TrackNodeLocation, Vec3>(location, normal)
	
	class TrainEndpointEdge(val graph: TrackGraph, val point: TravellingPoint) :
		Couple<TrackNode>(point.node1, point.node2) {
		val edge: TrackEdge? get() = graph.getConnectionsFrom(first).get(second)
		val edge2: TrackEdge? get() = graph.getConnectionsFrom(second).get(first)
	}
	
	class SplitDiscoveredPath(
		distance: Double,
		cost: Double,
		path: List<Couple<TrackNode>>,
		destination: GlobalStation,
		val graphs: Collection<TrackGraph>,
	) : DiscoveredPath(distance, cost, path, destination)
}
