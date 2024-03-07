package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.signal.LuaSignalEntry


@LuaObject
interface LuaTrackIntersection : LuaSignalEntry {
	@LuaProperty
	val edge1: LuaTrackEdge
	
	@LuaProperty
	val offset1: LuaTrackEdgeOffset
	
	@LuaProperty
	val edge2: LuaTrackEdge
	
	@LuaProperty
	val offset2: LuaTrackEdgeOffset
}
