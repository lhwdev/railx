package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty


@LuaObject
interface LuaTrainInfo {
	enum class Type { TODO }
	
	
	@LuaProperty
	val id: String
	
	@LuaProperty
	val name: String
	
	@LuaProperty
	val type: Type
}
