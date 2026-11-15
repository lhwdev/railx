@file:Suppress("DuplicatedCode")

package com.lhwdev.minecraft.utils.vectors

import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc


fun Vector3d.set(v: Vec3): Vector3d =
	set(v.x, v.y, v.z)

fun Vector3dc.add(v: Vec3, destination: Vector3d): Vector3d =
	add(v.x, v.y, v.z, destination)

fun Vector3d.add(v: Vec3): Vector3d =
	add(v.x, v.y, v.z)

fun Vector3d.setMul(v: Vec3, scalar: Double): Vector3d {
	x = v.x * scalar
	y = v.y * scalar
	z = v.z * scalar
	return this
}

fun Vector3d.setMul(v: Vector3d, scalar: Double): Vector3d {
	x = v.x * scalar
	y = v.y * scalar
	z = v.z * scalar
	return this
}

fun Vector3d.addMul(v: Vec3, scalar: Double): Vector3d {
	x += v.x * scalar
	y += v.y * scalar
	z += v.z * scalar
	return this
}

fun Vector3d.addMul(v: Vector3d, scalar: Double): Vector3d {
	x += v.x * scalar
	y += v.y * scalar
	z += v.z * scalar
	return this
}

fun Vector3d.lerp(other: Vec3, t: Double): Vector3d {
	x = Math.fma(other.x - x, t, x)
	y = Math.fma(other.y - y, t, y)
	z = Math.fma(other.z - z, t, z)
	return this
}

fun Vector3d.setLerp(self: Vec3, other: Vec3, t: Double): Vector3d {
	x = Math.fma(other.x - self.x, t, self.x)
	y = Math.fma(other.y - self.y, t, self.y)
	z = Math.fma(other.z - self.z, t, self.z)
	return this
}

fun Vector3d.setLerp(self: Vector3d, other: Vector3d, t: Double): Vector3d {
	x = Math.fma(other.x - self.x, t, self.x)
	y = Math.fma(other.y - self.y, t, self.y)
	z = Math.fma(other.z - self.z, t, self.z)
	return this
}
