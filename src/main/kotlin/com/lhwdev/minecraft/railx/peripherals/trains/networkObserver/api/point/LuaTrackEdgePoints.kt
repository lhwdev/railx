package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.point

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track.LuaTrackEdgePoint


object LuaTrackEdgePoints {
	@LuaObject
	interface SingleBlockPoint : LuaTrackEdgePoint
	
	@LuaObject
	interface Station : SingleBlockPoint {
		@LuaProperty
		override val handle: LuaStationHandle
	}
	
	@LuaObject
	interface Signal : SingleBlockPoint {
		@LuaProperty
		override val handle: LuaSignalHandle
	}
	
	@LuaObject
	interface Observer : SingleBlockPoint
}
