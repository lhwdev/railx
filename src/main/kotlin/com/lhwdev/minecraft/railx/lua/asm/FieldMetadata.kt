package com.lhwdev.minecraft.railx.lua.asm

interface FieldMetadata : ItemMetadata {
	val value: Any?
}

fun FieldMetadata(name: String, value: Any?): FieldMetadata = object : FieldMetadata {
	override val name: String = name
	override val value: Any? = value
}
