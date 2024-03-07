package com.lhwdev.minecraft.railx.cc

import dan200.computercraft.api.lua.ILuaContext
import org.squiddev.cobalt.LuaState


interface LuaContext : ILuaContext {
	fun issueLuaThreadTask(task: (state: LuaState) -> Unit)
	
	val isOnLuaThread: Boolean
}
