@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.utils.math

import kotlin.math.PI


/// (...) -> Angle
inline fun atan2(y: Double, x: Double): Angle =
	kotlin.math.atan2(y, x).radian


/// (Angle) -> ...
inline fun cos(angle: Angle): Double =
	kotlin.math.cos(angle.rad)

inline fun sin(angle: Angle): Double =
	kotlin.math.sin(angle.rad)

infix fun Angle.distanceTo(other: Angle): Double {
	val diff = (this - other).unboundedRadian()
	return when {
		diff > PI2 -> 0.0
		diff > PI -> PI2 - diff
		diff < 0 -> 0.0
		else -> diff
	}
}


// Note: inline fun cos(angle: interface AngleLike) causes boxing/unboxing even with inline.

/// (Incline) -> ...
inline fun cos(angle: Incline): Double =
	kotlin.math.cos(angle.radian)

inline fun sin(angle: Incline): Double =
	kotlin.math.sin(angle.radian)
