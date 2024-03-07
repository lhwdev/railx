package com.lhwdev.minecraft.railx.cc

import com.lhwdev.minecraft.railx.lua.LuaExecutionContext
import dan200.computercraft.core.computer.Computer
import org.squiddev.cobalt.LuaValue
import org.squiddev.cobalt.Varargs
import org.squiddev.cobalt.function.LuaFunction


interface LuaBridgeContext : LuaBridgeFunctionContext {
	val computer: Computer
}


fun Any?.toLuaValue(): LuaValue =
	LuaExecutionContext.current.toLuaValue(this)

fun LuaValue.toRealValue(): Any? =
	LuaExecutionContext.current.fromLuaValue(this)

fun createFunction(block: suspend LuaFunctionContext.(args: Varargs) -> Varargs): LuaFunction =
	LuaExecutionContext.current.bridge.createFunction(block)
