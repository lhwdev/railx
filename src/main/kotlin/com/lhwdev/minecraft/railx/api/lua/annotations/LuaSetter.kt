package com.lhwdev.minecraft.railx.api.lua.annotations


@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class LuaSetter(
	/**
	 * Explicitly specify the property name of this accessor. If not given, it uses the name of the annotated method.
	 */
	val value: String = "",
	
	/**
	 * Makes getter 'dynamic', which make this item to be resolved using `__index` on runtime, instead of being
	 * flattened into objects.
	 */
	val dynamic: Boolean = false,
)
