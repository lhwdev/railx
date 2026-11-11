@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")

package com.lhwdev.minecraft.railx.utils.math

import kotlin.math.PI
import kotlin.math.floor


const val PI2 = PI * 2.0

const val InvPI2 = 1.0 / PI2

const val RadToDeg = 180.0 / PI

const val DegToRad = PI / 180.0


/**
 * Small value is expected. When `this` is significantly large, precision might be lowered.
 */
inline val Double.radian: Angle
	get() = Angle(rad = this)

inline val Float.radian: Angle
	get() = Angle(rad = toDouble())

val Double.degree: Angle
	get() = (this * DegToRad).radian


interface AngleLike {
	val rad: Double
}


/**
 * ## Units and values
 * - [rad]: radian, unbounded.
 * - [radian]: radian, bounded to `[0, 2pi)`.
 * - [degree]: degree, bounded to `[0, 360)`.
 * - [fraction]: 1 means full turn (2pi); 0.5 is half. bounded to `[0, 1)`.
 * - `fraction2`: `fraction * 2`; `[0, 2)`.
 * - `halfFraction`: `fraction2 % 1.0`; `[0, 1)`.
 */
@JvmInline
value class Angle(
	/**
	 * Value in radian, which is not bounded.
	 */
	override val rad: Double,
) : AngleLike {
	companion object {
		val Zero: Angle = 0.0.radian
		val Pi: Angle = PI.radian
		
		fun fromFraction(fraction: Double): Angle =
			Angle(rad = fraction * PI2)
		
		fun fromFraction2(fraction2: Double): Angle =
			Angle(rad = fraction2 * PI)
	}
	
	
	inline val radF: Float
		get() = rad.toFloat()
	
	
	/**
	 * Strictly bounded to `[0, 2pi)`.
	 */
	val radian: Double
		get() {
			val radian = rad - PI2 * floor(InvPI2 * rad)
			return when {
				radian >= PI2 -> 0.0
				radian < 0.0 -> 0.0
				else -> radian
			}
		}
	
	inline fun unboundedRadian(): Double =
		rad - PI2 * floor(InvPI2 * rad)
	
	/**
	 * Strictly bounded to `[0, 1)`.
	 */
	val fraction: Double
		get() {
			var fraction = rad / PI2
			fraction -= floor(fraction)
			return when {
				fraction >= 1.0 -> 0.0
				fraction < 0.0 -> 0.0
				else -> fraction
			}
		}
	
	/**
	 * PI or 180deg corresponds to `1.0`. Strictly bounded to `[0, 1)`.
	 */
	val halfFraction: Double
		get() {
			val fraction = radian / PI
			return if(fraction >= 2.0) 0.0 else fraction
		}
	
	/**
	 * Strictly bounded to `[0, 360)`.
	 */
	val degree: Double
		get() {
			val degree = unboundedRadian() * RadToDeg
			return if(degree >= 360.0) 0.0 else degree
		}
	
	val halfDegree: Double
		get() = degree % 180.0
	
	
	inline operator fun unaryMinus(): Angle =
		Angle(-rad)
	
	inline operator fun plus(other: Angle): Angle =
		Angle(radian + other.radian)
	
	inline operator fun minus(other: Angle): Angle =
		plus(-other)
	
	inline operator fun times(other: Double): Angle =
		Angle(radian * other)
	
	inline operator fun div(other: Double): Angle =
		Angle(radian / other)
}
