@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.utils


inline fun Int.pow2(): Int =
	this * this

inline infix fun Int.floorMod(by: Int): Int =
	Math.floorMod(this, by)

infix fun Double.floorMod(by: Double): Double {
	val r = this % by
	
	return if(this * by < 0 && r != 0.0) r + by
	else r
}

infix fun Double.similarTo(to: Double): Boolean =
	(this - to) < 1e-10
