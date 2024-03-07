package com.lhwdev.minecraft.railx.api.lua.annotations


@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class LuaField(
	/**
	 * Explicitly specify the name of this field. If not given, it uses the name of the annotated property.
	 */
	val value: String = "",
)
