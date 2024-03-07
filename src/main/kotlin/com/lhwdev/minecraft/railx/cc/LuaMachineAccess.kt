package com.lhwdev.minecraft.railx.cc

import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.core.lua.ILuaMachine
import org.squiddev.cobalt.LuaState
import org.squiddev.cobalt.LuaValue
import org.squiddev.cobalt.Varargs
import java.util.*


interface LuaMachineAccess : ILuaMachine {
	val context: ILuaContext
	
	val state: LuaState
	
	val objectProvider: LuaObjectProvider
	
	
	fun convertToValue(value: Any?, cache: IdentityHashMap<Any?, LuaValue>? = null): LuaValue
	
	fun convertFromValue(value: LuaValue, cache: IdentityHashMap<LuaValue, Any?>? = null): Any?
	
	fun convertToValues(objects: Array<out Any?>?): Varargs
}
