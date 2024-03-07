package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty


@LuaObject
interface LuaCarriageConnection {
	@LuaProperty
	val from: LuaCarriage
	
	@LuaProperty
	val to: LuaCarriage
	
	
}
