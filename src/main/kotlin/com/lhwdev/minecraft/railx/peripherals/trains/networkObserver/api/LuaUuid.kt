package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaExtra
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import java.util.*


@LuaObject
class LuaUuid(val uuid: UUID) {
	@LuaExtra
	override fun toString(): String = uuid.toString()
}
