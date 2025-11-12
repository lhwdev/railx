@file:JvmName("TrackGraphForSplitUtils")

package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.Create
import com.simibubi.create.content.trains.GlobalRailwayManager
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNode
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import net.minecraft.world.phys.Vec3
import java.util.*


@Suppress("FunctionName")
interface TrackGraphForSplit {
	fun `railx$getConnectedId`(): UUID?
	
	fun `railx$setConnectedId`(id: UUID?)
	
	fun `railx$addCreatedNode`(node: TrackNode)
	
	fun `railx$loadSplittingNode`(
		data: SplittingTrackNode.Data,
		location: TrackNodeLocation,
		netId: Int,
		normal: Vec3,
	)
	
	fun `railx$connectedGraphs`(): Collection<UUID>
	
	fun `railx$allConnectedGraphsIncludingSelf`(manager: GlobalRailwayManager): Set<TrackGraph>
}


var TrackGraph.connectedId: UUID?
	get() = (this as TrackGraphForSplit).`railx$getConnectedId`()
	set(value) {
		(this as TrackGraphForSplit).`railx$setConnectedId`(value)
	}

val TrackGraph.connectedGraphs: Collection<UUID>
	get() = (this as TrackGraphForSplit).`railx$connectedGraphs`()

fun TrackGraph.allConnectedGraphsIncludingSelfImpl(manager: GlobalRailwayManager): Set<TrackGraph> {
	val networks = (if(manager == Create.RAILWAYS) RailwayServerGlobals.trackNetworks else null)
		?: manager.trackNetworks
	
	val result = mutableSetOf<TrackGraph>()
	val frontier = ArrayDeque<TrackGraph>().also { it += this }
	while(frontier.isNotEmpty()) {
		val current = frontier.removeFirst()
		if(!result.add(current)) continue
		
		current.connectedGraphs.mapNotNullTo(frontier) { networks[it] }
	}
	return result
}

fun TrackGraph.allConnectedGraphsIncludingSelf(manager: GlobalRailwayManager): Set<TrackGraph> =
	(this as TrackGraphForSplit).`railx$allConnectedGraphsIncludingSelf`(manager)

fun TrackGraph.addCreatedNode(node: TrackNode) {
	(this as TrackGraphForSplit).`railx$addCreatedNode`(node)
}
