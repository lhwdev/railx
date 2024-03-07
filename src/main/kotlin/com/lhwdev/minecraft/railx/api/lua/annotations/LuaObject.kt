package com.lhwdev.minecraft.railx.api.lua.annotations


@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class LuaObject(
	/**
	 * Whether to resolve the value with the same object with class annotated by `LuaObject`, dynamically.
	 * This applies to field, property, return value of function.
	 */
	val dynamic: Boolean = true,
	
	/**
	 * If [data] is true and this class is data class, operates in data mode, which enables built-in method to
	 * deserialize LuaObject from lua table.
	 */
	val data: Boolean = true,
) {
	@MustBeDocumented
	@Retention(AnnotationRetention.RUNTIME)
	@Target(AnnotationTarget.FUNCTION)
	annotation class Deserializer
	
	@MustBeDocumented
	@Retention(AnnotationRetention.RUNTIME)
	@Target(AnnotationTarget.CLASS)
	annotation class SealedVariants(
		val key: String,
	)
	
	@MustBeDocumented
	@Retention(AnnotationRetention.RUNTIME)
	@Target(AnnotationTarget.CLASS)
	annotation class SealedVariant(
		/**
		 * Default variantName is named after `class.simpleName.firstToLowercase()`.
		 */
		val variantName: String = "",
	)
}
