package com.lhwdev.minecraft.railx.utils

import com.lhwdev.minecraft.utils.vectors.minus
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3


fun Vec3.isNormalized(): Boolean =
	(length() - 1.0) < 1e-5

fun Mirror.mirror(vector: Vec3): Vec3 = when(this) {
	Mirror.NONE -> vector
	Mirror.LEFT_RIGHT -> Vec3(-vector.x, vector.y, vector.z)
	Mirror.FRONT_BACK -> Vec3(vector.x, vector.y, -vector.z)
}

fun Rotation.rotate(vector: Vec3): Vec3 = when(this) {
	Rotation.NONE -> vector
	Rotation.CLOCKWISE_90 -> Vec3(vector.z, vector.y, -vector.x)
	Rotation.CLOCKWISE_180 -> Vec3(-vector.x, vector.y, -vector.z)
	Rotation.COUNTERCLOCKWISE_90 -> Vec3(-vector.z, vector.y, vector.x)
}

infix fun Vec3.closeTo(other: Vec3): Boolean =
	this === other || (this - other).lengthSqr() < 1e-12

fun Vec3.isInt(): Boolean =
	x.isInt() && y.isInt() && z.isInt()

private fun Double.isInt(): Boolean =
	this similarTo toInt().toDouble()
