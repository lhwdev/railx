package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty

@LuaObject
interface LuaTrackEdgePoint : LuaTrackLocation {
	@LuaField
	val id: String
	
	@LuaField
	val type: String
	
	@LuaProperty(dynamic = true)
	val handle: Any
}
