package com.lhwdev.minecraft.railx.lua.asm

import com.lhwdev.minecraft.railx.api.lua.LuaArguments

interface MethodMetadata : ItemMetadata {
	suspend fun invoke(self: Any, arguments: LuaArguments): Any?
}
