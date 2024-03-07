package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty


@LuaObject
class LuaLocation(
	@LuaProperty
	val x: Float,
	
	@LuaProperty
	val y: Float,
	
	@LuaProperty
	val z: Float,
	
	@LuaProperty
	val dimension: String,
)

@LuaObject
class LuaVec3(
	@LuaProperty
	val x: Float,
	
	@LuaProperty
	val y: Float,
	
	@LuaProperty
	val z: Float,
)
