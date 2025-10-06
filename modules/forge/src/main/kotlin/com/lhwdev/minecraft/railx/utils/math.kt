@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.utils

import kotlin.math.abs


inline fun Int.pow2(): Int =
	this * this

inline fun Double.pow2(): Double =
	this * this

inline infix fun Int.floorMod(by: Int): Int =
	Math.floorMod(this, by)

infix fun Double.floorMod(by: Double): Double {
	val r = this % by
	
	return if(this * by < 0 && r != 0.0) r + by
	else r
}

inline infix fun Double.similarTo(to: Double): Boolean =
	abs(this - to) < 1e-10
