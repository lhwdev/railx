package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.LuaLocation
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.LuaVec3
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.signal.LuaSignalGroup

@LuaObject
interface LuaTrackLocation {
	@LuaField
	val trackEdge: LuaTrackEdge
	
	@LuaField
	val offset: LuaTrackEdgeOffset
	
	@LuaProperty
	val location: LuaLocation
	
	@LuaProperty
	val direction: LuaVec3
	
	@LuaFunction
	fun findSignalGroup(): LuaSignalGroup
	
}
