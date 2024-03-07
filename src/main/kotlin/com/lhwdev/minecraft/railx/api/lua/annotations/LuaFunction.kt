package com.lhwdev.minecraft.railx.api.lua.annotations


@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class LuaFunction(
	/**
	 * Explicitly specify the name of this function. If not given, it uses the name of the annotated property.
	 */
	val value: String = "",
)
