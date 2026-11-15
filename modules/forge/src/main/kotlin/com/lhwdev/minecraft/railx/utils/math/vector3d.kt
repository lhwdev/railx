package com.lhwdev.minecraft.railx.utils.math

import com.lhwdev.minecraft.utils.vectors.setLerp
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import kotlin.math.acos
import kotlin.math.sin


fun Vector3d.normalizeOrZero(): Vector3d {
	val length = length()
	return if(length < 1e-4) zero() else mul(1.0 / length)
}


interface Vector3dInterpolation {
	fun interpolateTo(t: Double, dest: Vector3d = Vector3d()): Vector3d
}

class Vector3dLerp(private val from: Vec3, private val to: Vec3) : Vector3dInterpolation {
	override fun interpolateTo(t: Double, dest: Vector3d): Vector3d =
		dest.setLerp(from, to, t)
}

/**
 * [from] and [to] should not be mutated.
 */
class Vector3dSlerp(private val from: Vec3, private val to: Vec3) : Vector3dInterpolation {
	private val theta = acos(from.dot(to))
	private val inverseSinTheta = 1.0 / sin(theta)
	
	override fun interpolateTo(t: Double, dest: Vector3d): Vector3d {
		val first = sin((1 - t) * theta)
		val second = sin(theta * t)
		return dest.set(
			(from.x * first + to.x * second) * inverseSinTheta,
			(from.y * first + to.y * second) * inverseSinTheta,
			(from.z * first + to.z * second) * inverseSinTheta,
		)
	}
}
