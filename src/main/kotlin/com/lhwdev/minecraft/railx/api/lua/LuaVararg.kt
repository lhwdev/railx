package com.lhwdev.minecraft.railx.api.lua


interface LuaVararg {
	val values: Array<out Any?>
}

fun LuaVararg(vararg values: Any?): LuaVararg = object : LuaVararg {
	override val values: Array<out Any?> =
		values
}
