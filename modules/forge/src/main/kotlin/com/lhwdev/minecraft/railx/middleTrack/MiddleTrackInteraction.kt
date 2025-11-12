package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.other.toTrackEdge
import com.lhwdev.minecraft.railx.utils.getOrDefault
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.graph.TrackGraphLocation
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem.OverlapResult
import net.createmod.catnip.data.Couple
import net.minecraft.world.level.Level
import kotlin.math.abs

object MiddleTrackInteraction {
	val enabled: Boolean
		get() = RailXConfig.Server.middleTrack.enabled.getOrDefault(false) &&
			RailXConfig.Server.middleTrack.enableInteraction.getOrDefault(false)
	
	
	class GraphLocation(val result: OverlapResult, val location: TrackGraphLocation? = null)
	
	fun withGraphLocation(
		level: Level,
		curve: BezierConnection,
		targetBezier: BezierTrackPointLocation,
		type: EdgePointType<*>,
		front: Boolean,
	): GraphLocation {
		var (graph, edge) = curve.toTrackEdge(level) ?: return GraphLocation(OverlapResult.NO_TRACK)
		
		var position = if(curve.usingLegacySegment()) {
			(targetBezier.segment + 1) / 2.0
		} else {
			curve.getSegmentT(targetBezier.segment + 1) / 2.0
		}
		if(front) {
			edge = graph.getConnection(Couple.create(edge.node2, edge.node1))
			position = edge.length - position
		}
		
		val location = TrackGraphLocation()
		location.graph = graph
		location.edge = Couple.create(edge.node1.location, edge.node2.location)
		location.position = position
		
		for(edgePoint in edge.edgeData.points) {
			val otherEdgePosition = edgePoint.getLocationOn(edge)
			val distance = abs(position - otherEdgePosition)
			if(distance > .75) continue
			if(edgePoint.canCoexistWith(type, front) && distance < .25) continue
			
			return GraphLocation(OverlapResult.OCCUPIED, location)
		}
		
		return GraphLocation(OverlapResult.VALID, location)
	}
}

fun BezierConnection.usingLegacySegment(): Boolean =
	material !is FlexiTrackMaterial
