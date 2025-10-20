@file:JvmName("BezierConnectionUtils")

package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.utils.pow3
import com.simibubi.create.content.trains.track.BezierConnection
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.times


internal interface IBezierConnectionExtension {
	fun minRadius(): Double
}

internal fun BezierConnection.calculateMinRadius(): Double {
	var minRadius = Double.POSITIVE_INFINITY
	val p1 = starts.first
	val p2 = starts.second
	val q1 = p1 + axes.first * handleLength
	val q2 = p2 + axes.second * handleLength
	
	for(segment in 0..segmentCount) {
		val t = getSegmentT(segment).toDouble()
		
		val derivative = p1.scale(-3 * t * t + 6 * t - 3)
			.add(q1.scale(9 * t * t - 12 * t + 3))
			.add(q2.scale(-9 * t * t + 6 * t))
			.add(p2.scale(3 * t * t))
		val derivative2 = p1.scale(-6 * t + 6)
			.add(q1.scale(18 * t - 12))
			.add(q2.scale(-18 * t + 6))
			.add(p2.scale(6 * t))
		
		val denominator = derivative.cross(derivative2).length()
		if(denominator < 1e-6) continue
		val radius = derivative.length().pow3() / denominator
		if(radius < minRadius) {
			minRadius = radius
		}
	}
	return minRadius
}

fun BezierConnection.minRadius(): Double =
	(this as IBezierConnectionExtension).minRadius()

fun BezierConnection.derivative(t: Double): Vec3 {
	val p1 = starts.first
	val p2 = starts.second
	val q1 = p1 + axes.first * handleLength
	val q2 = p2 + axes.second * handleLength
	return p1.scale(-3 * t * t + 6 * t - 3)
		.add(q1.scale(9 * t * t - 12 * t + 3))
		.add(q2.scale(-9 * t * t + 6 * t))
		.add(p2.scale(3 * t * t))
}

fun BezierConnection.derivative2(t: Double): Vec3 {
	val p1 = starts.first
	val p2 = starts.second
	val q1 = p1 + axes.first * handleLength
	val q2 = p2 + axes.second * handleLength
	return p1.scale(-6 * t + 6)
		.add(q1.scale(18 * t - 12))
		.add(q2.scale(-18 * t + 6))
		.add(p2.scale(6 * t))
}
