package com.lhwdev.minecraft.railx.api.lua.annotations


@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class LuaProperty(
	/**
	 * Explicitly specify the property name of this accessor. If not given, it uses the name of the annotated property.
	 */
	val value: String = "",
	
	val type: Type = Type.Default,
	
	/**
	 * Marks whether to use dynamic type resolution.
	 */
	val dynamic: Boolean = true,
	
	/**
	 * Makes property exposed to kotlin as 'var', readonly to lua.
	 */
	val readonly: Boolean = false,
) {
	enum class Type { Default, Field, Accessor }
}
