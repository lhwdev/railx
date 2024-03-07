package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.LuaLocation
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.LuaVec3


@LuaObject
interface LuaTrackNode {
	@LuaField
	val type: String
	
	@LuaField
	val id: String
	
	@LuaProperty
	val location: LuaLocation
	
	@LuaProperty
	val direction: LuaVec3
}
