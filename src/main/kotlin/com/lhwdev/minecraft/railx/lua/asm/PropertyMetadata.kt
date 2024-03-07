package com.lhwdev.minecraft.railx.lua.asm

interface PropertyMetadata : ItemMetadata {
	val isMutable: Boolean
	
	suspend fun get(self: Any): Any?
	
	suspend fun set(self: Any, value: Any?)
}
