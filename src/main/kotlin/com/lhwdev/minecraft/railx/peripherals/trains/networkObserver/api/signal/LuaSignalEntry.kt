package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.signal

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject


@LuaObject
interface LuaSignalEntry {
	@LuaFunction
	fun findSignalGroup(): LuaSignalGroup
}
