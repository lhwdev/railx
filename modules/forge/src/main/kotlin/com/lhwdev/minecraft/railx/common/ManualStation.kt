package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.splitGraph.MovingTravellingPoint
import com.lhwdev.minecraft.railx.utils.allTravellingPoints
import com.lhwdev.minecraft.railx.utils.component1
import com.lhwdev.minecraft.railx.utils.component2
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.station.GlobalStation
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max


object ManualStation {
	class Approaching(val station: GlobalStation, val distance: Double)
	
	private class Frontier(val edge: TrackEdge, val distance: Double) : Comparable<Frontier> {
		override fun compareTo(other: Frontier): Int {
			TODO("Not yet implemented")
		}
	}
	
	fun findApproachingStation(train: Train, forward: Boolean, forwardControl: Boolean): Approaching? {
		val graph = train.graph ?: return null
		
		val acceleration = train.acceleration().toDouble()
		val minDistance = 0.75 * (train.speed * train.speed) / (2 * acceleration)
		val maxDistance = max(32.0, 1.5 * (train.speed * train.speed) / (2 * acceleration))
		var result: Approaching? = null
		
		train.navigation.search(
			maxDistance, forward, null,
		) { distance, _, _, currentEntry, globalStation ->
			if(distance < minDistance) return@search false
			val edge = currentEntry.getSecond()
			val distanceToStation = distance - edge.length + globalStation.getLocationOn(edge)
			if(distanceToStation < minDistance) return@search false
			
			val presentTrain = globalStation.presentTrain
			if(presentTrain != null && presentTrain != train) return@search false
			result = Approaching(globalStation, distanceToStation)
			true
		}
		
		
		// In case of overrun
		val overrunLimit = RailXConfig.Server.common.manualStationDistanceLimit.asDouble + 2.0
		if(result == null && minDistance <= overrunLimit) {
			val points = train.allTravellingPoints.toMutableList()
			if(!forwardControl) points.reverse()
			
			val point = MovingTravellingPoint(points.first())
			point.reverse(graph)
			
			points.removeFirst()
			var distance = 0.0
			val edgePointListener: TravellingPoint.IEdgePointListener = { d, couple ->
				val (station, nodes) = couple
				val (from, _) = nodes
				if(station is GlobalStation && station.canApproachFrom(from)) {
					result = Approaching(station, -(distance + d))
					true
				} else false
			}
			
			for(next in points) {
				distance += point.travel(graph, overrunLimit - distance, point.follow(next), edgePointListener)
				if(distance >= overrunLimit) break
				if(result != null) break
			}
			
			if(distance < overrunLimit) point.travel(
				graph,
				overrunLimit - distance,
				point.steer(TravellingPoint.SteerDirection.NONE, Vec3(0.0, 1.0, 0.0)),
				edgePointListener,
			)
		}
		
		return result
	}
	
	
	fun canDisassemble(train: Train): Boolean {
		val station = train.getCurrentStation() ?: return false
		return when {
			pointCloseEnoughForDisassemble(station, train, train.carriages.first().leadingPoint) -> true
			pointCloseEnoughForDisassemble(station, train, train.carriages.last().trailingPoint) -> true
			else -> false
		}
	}
	
	private fun pointCloseEnoughForDisassemble(station: GlobalStation, train: Train, point: TravellingPoint): Boolean {
		val graph = train.graph
		val node1 = graph.locateNode(station.edgeLocation.first)
		val node2 = graph.locateNode(station.edgeLocation.second)
		
		val distanceLimit = RailXConfig.Server.common.manualStationDisassembleLimit.asDouble
		
		if(point.node1 == node1 && point.node2 == node2)
			return abs(point.position - station.position) < distanceLimit
		if(point.node1 == node2 && point.node2 == node1)
			return abs(point.position + station.position - point.edge.length) < distanceLimit
		return false
	}
}
