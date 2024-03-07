package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty


typealias LuaTrackEdgeOffset = Double


@LuaObject
interface LuaTrackEdge {
	@LuaField
	val node1: LuaTrackNode
	
	@LuaField
	val node2: LuaTrackNode
	
	@LuaProperty
	val points: List<LuaTrackEdgePoint>
	
	@LuaProperty
	val intersections: List<LuaTrackIntersection>
	
	@LuaProperty
	val length: Double
	
	/**
	 * If equals to 0, this edge is straight line.
	 */
	@LuaProperty
	val maxCurvature: Double
	
	@LuaProperty
	val trackKind: LuaTrackKind
	
	@LuaFunction
	fun at(offset: LuaTrackEdgeOffset): LuaTrackLocation
	
	@LuaFunction
	fun partial(start: Double, end: Double): LuaTrackPartialEdge
}


@LuaObject
interface LuaTrackPartialEdge {
	@LuaField
	val edge: LuaTrackEdge
	
	@LuaField
	val start: Double
	
	@LuaField
	val end: Double
}
