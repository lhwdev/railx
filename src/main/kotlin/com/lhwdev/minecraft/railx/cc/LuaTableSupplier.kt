package com.lhwdev.minecraft.railx.cc

import com.lhwdev.minecraft.railx.lua.asm.ObjectMetadata


interface LuaTableSupplier {
	fun getObjectMetadata(value: Any): ObjectMetadata
}
