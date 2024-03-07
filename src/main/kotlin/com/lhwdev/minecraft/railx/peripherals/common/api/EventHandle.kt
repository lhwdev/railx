package com.lhwdev.minecraft.railx.peripherals.common.api

import com.lhwdev.minecraft.railx.api.lua.LuaVararg
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.lua.LuaExecutionContext
import org.squiddev.cobalt.LuaValue


@LuaObject
interface EventHandle {
	val name: String
	
	val id: LuaValue
	
	
	@LuaFunction
	suspend fun waitEvent(): LuaVararg =
		LuaExecutionContext.current.waitEvent(name) { it.values[0] == id }
	
	@LuaFunction
	fun unsubscribe()
}
