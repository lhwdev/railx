package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver

import com.simibubi.create.Create
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackGraphLocation
import com.simibubi.create.content.trains.observer.TrackObserver
import com.simibubi.create.content.trains.signal.SignalBoundary
import com.simibubi.create.content.trains.station.GlobalStation
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour


class TrainNetworkObserverBehavior(val be: TrainNetworkObserverBlockEntity) : BlockEntityBehaviour(be) {
	
	companion object {
		val Type = BehaviourType<TrainNetworkObserverBehavior>()
	}
	
	
	val edgePoint get() = be.edgePoint
	
	val graphLocation: TrackGraphLocation
		get() = edgePoint.determineGraphLocation()
	val graph: TrackGraph
		get() = graphLocation.graph
	
	val trains: List<Train>
		get() = Create.RAILWAYS.trains.values.filter { it.graph == graph }
	
	val stations: Collection<GlobalStation>
		get() = graph.getPoints(EdgePointType.STATION)
	val signals: Collection<SignalBoundary>
		get() = graph.getPoints(EdgePointType.SIGNAL)
	val observers: Collection<TrackObserver>
		get() = graph.getPoints(EdgePointType.OBSERVER)
	
	
	override fun getType() = Type
}
