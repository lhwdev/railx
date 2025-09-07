@file:Suppress("ReplacePutWithAssignment")

package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.utils.component1
import com.lhwdev.minecraft.railx.utils.component2
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos


object MiddleTracks {
	val TracksFromMiddle: WorldAttached<MutableMap<BlockPos, MutableMap<BlockPos, MutableList<MiddleBezierConnection>>>> =
		WorldAttached { mutableMapOf() }
	
	fun registerMiddleTrack(be: MiddleTrackBlockEntity) {
		val tracks = TracksFromMiddle.get(be.level!!)
		for(connection in be.connections) {
			val (from, to) = connection.bePositions
			tracks.getOrPut(from) { mutableMapOf() }
				.getOrPut(to) { mutableListOf() }
				.add(MiddleBezierConnection(connection, middlePos = be.blockPos))
		}
	}
	
	fun unregisterMiddleTrack(be: MiddleTrackBlockEntity) {
		val tracks = TracksFromMiddle.get(be.level!!)
		val allConnections = be.connections.groupBy { it.bePositions.first }
		
		for((from, toConnections) in allConnections) {
			val previous = tracks[from] ?: continue
			for(connection in toConnections) {
				val to = connection.bePositions.second
				val previous2 = previous[to] ?: continue
				previous2.removeAt(previous2.indexOfFirst { it.middlePos == be.blockPos })
				if(previous2.isEmpty()) previous -= to
			}
			if(previous.isEmpty()) tracks -= from
		}
	}
}