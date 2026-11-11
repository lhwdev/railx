@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")

package com.lhwdev.minecraft.railx.utils.math


inline fun Angle.asIncline(): Incline =
	Incline(radian = rad)


@JvmInline
value class Incline(
	/**
	 * Angle in radian, strictly inside `[-pi/2, pi/2]`.
	 */
	val radian: Double,
) : AngleLike {
	companion object {
		val Zero: Incline = Incline(radian = 0.0)
	}
	
	@Deprecated(message = "use radian", replaceWith = ReplaceWith("radian"))
	override inline val rad: Double
		get() = radian
	
	inline val radianF: Float
		get() = radian.toFloat()
	
	inline val degree: Double
		get() = radian * RadToDeg
}
