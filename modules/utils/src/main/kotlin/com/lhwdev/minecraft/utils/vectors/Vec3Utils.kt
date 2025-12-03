@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.utils.vectors

import net.minecraft.world.phys.Vec3


// operations

inline operator fun Vec3.unaryPlus(): Vec3 =
	this

inline operator fun Vec3.unaryMinus(): Vec3 =
	scale(-1.0)


inline operator fun Vec3.plus(other: Vec3): Vec3 =
	add(other)

inline operator fun Vec3.minus(other: Vec3): Vec3 =
	subtract(other)

inline operator fun Vec3.times(other: Vec3): Vec3 =
	multiply(other)

inline operator fun Vec3.times(scalar: Double): Vec3 =
	scale(scalar)

