package com.lhwdev.minecraft.railx.api.lua.annotations

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class LuaExtra(
	/**
	 * If this value is true, field is dynamically resolved.
	 */
	val dynamic: Boolean = false,
	
	val target: Target = Target.Self,
) {
	enum class Target { Self, Metatable }
}
