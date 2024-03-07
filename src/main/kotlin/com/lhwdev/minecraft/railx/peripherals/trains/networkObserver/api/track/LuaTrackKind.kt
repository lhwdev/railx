package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty


@LuaObject
interface LuaTrackKind {
	@LuaProperty
	val name: String
	
	@LuaProperty
	val width: Double
}
