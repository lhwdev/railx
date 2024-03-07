package com.lhwdev.minecraft.railx.peripherals.common

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject

@LuaObject
interface LuaIndexedGraph<N : Any, E : Any> : LuaGraph<N, E>, List<N>
