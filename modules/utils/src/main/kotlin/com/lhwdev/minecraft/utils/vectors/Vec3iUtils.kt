@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.utils.vectors

import net.minecraft.core.Vec3i


// operations

inline operator fun Vec3i.plus(other: Vec3i): Vec3i =
	offset(other)

inline operator fun Vec3i.minus(other: Vec3i): Vec3i =
	subtract(other)
