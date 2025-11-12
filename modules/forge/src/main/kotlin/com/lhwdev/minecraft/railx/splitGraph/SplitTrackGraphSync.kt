@file:JvmName("SplitTrackGraphSyncUtils")

package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.content.trains.graph.EdgeData
import com.simibubi.create.content.trains.graph.TrackGraph
import java.util.*


@Suppress("FunctionName")
interface SplitTrackGraphSync {
	companion object {
		@JvmField
		val NullConnectedId: UUID = EdgeData.passiveGroup
	}
	
	fun `railx$connectedIdChanged`(graph: TrackGraph)
}


@Suppress("FunctionName")
interface SplitTrackGraphSyncPacket {
	fun `railx$setConnectedId`(id: UUID?)
}
