@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.utils

import com.lhwdev.minecraft.utils.vectors.toVec3
import com.lhwdev.minecraft.utils.vectors.toVector3d
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniondc
import kotlin.math.abs
import kotlin.math.round


fun sign(value: Int): Int = when {
	value == 0 -> 0
	value > 0 -> 1
	else -> -1
}


inline fun Int.pow2(): Int =
	this * this

inline fun Double.pow2(): Double =
	this * this

inline fun Double.pow3(): Double =
	this * this * this

inline infix fun Int.floorMod(by: Int): Int =
	Math.floorMod(this, by)

infix fun Double.floorMod(by: Double): Double {
	val r = this % by
	
	return if(this * by < 0 && r != 0.0) r + by
	else r
}

fun round(value: Double, points: Int): Double =
	round(value * points) / points

inline infix fun Double.similarTo(to: Double): Boolean =
	abs(this - to) < 1e-10


fun Quaterniondc.transform(vec: Vec3): Vec3 = transform(vec.toVector3d()).toVec3()

fun Quaterniondc.transformUnit(vec: Vec3): Vec3 = transformUnit(vec.toVector3d()).toVec3()
