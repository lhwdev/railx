package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.api.peripheral.PeripheralBase


class TrainNetworkObserverPeripheral(private val blockEntity: TrainNetworkObserverBlockEntity) : PeripheralBase() {
	override val peripheralName: String
		get() = "train_network_observer"
	
	@LuaFunction
	fun hoi(): String {
		return "This is my kingdom coom"
	}
	
	@LuaProperty
	val ho: String get() = "No matter what we breed,"
}


// class TrainNetworkObserverPeripheral(private val be: TrainNetworkObserverBlockEntity) : PeripheralBase() {
// 	override val peripheralName get() = "railx:train_network_observer"
//
// 	override fun provideObject() = Main()
//
//
// 	val graph: TrackGraph
// 		get() = TODO()
//
//
// 	@LuaObject
// 	inner class Main {
// 		@LuaProperty
// 		val pathGraph = PeripheralTrackPathGraph()
//
// 		@LuaProperty
// 		val blockGraph = PeripheralTrackBlockGraph()
//
// 		@LuaProperty
// 		val edgePointsGraph = PeripheralEdgePointsGraph()
//
// 		@LuaProperty
// 		val currentEdge = PeripheralCurrentEdge()
// 	}
//
// 	@LuaObject
// 	inner class PeripheralTrackPathGraph {
//
// 	}
//
// 	@LuaObject
// 	inner class PeripheralEdgePointsGraph {
//
// 	}
//
// 	@LuaObject
// 	inner class PeripheralCurrentEdge {
//
// 	}
// }
//
//
// private class Graph
