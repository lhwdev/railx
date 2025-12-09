@file:JvmName("BezierConnectionUtils")

package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.compat.copiedFrom
import com.lhwdev.minecraft.railx.utils.closeTo
import com.lhwdev.minecraft.railx.utils.pow3
import com.lhwdev.minecraft.utils.vectors.plus
import com.lhwdev.minecraft.utils.vectors.times
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Couple
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d


inline val BezierConnection.from: BlockPos
	get() = bePositions.first

inline val BezierConnection.to: BlockPos
	get() = bePositions.second

fun BezierConnection.asPrimary(): BezierConnection =
	if(primary) this else secondary()

val BezierConnection.primaryPositions: Couple<BlockPos>
	get() = if(primary) bePositions else bePositions.swap()


internal interface IBezierConnectionExtension {
	/**
	 * Only accounts x and z components.
	 */
	fun minRadius(): Double
}


private fun Vec3.toFlat(): Vector3d = Vector3d(x, 0.0, z)

fun BezierConnection.radiusAt(t: Double): Double {
	val p1 = starts.first.toFlat()
	val p2 = starts.second.toFlat()
	val q1 = axes.first.toFlat().mul(handleLength).add(p1)
	val q2 = axes.second.toFlat().mul(handleLength).add(p2)
	
	val derivative = Vector3d()
	derivative.fma(-3 * t * t + 6 * t - 3, p1)
	derivative.fma(9 * t * t - 12 * t + 3, q1)
	derivative.fma(-9 * t * t + 6 * t, q2)
	derivative.fma(3 * t * t, p2)
	
	val derivative2 = Vector3d()
	derivative2.fma(-6 * t + 6, p1)
	derivative2.fma(18 * t - 12, q1)
	derivative2.fma(-18 * t + 6, q2)
	derivative2.fma(6 * t, p2)
	
	val derivativeLength = derivative.length().pow3()
	
	derivative.cross(derivative2)
	val denominator = derivative.length()
	if(denominator < 1e-6) return Double.POSITIVE_INFINITY
	return derivativeLength / denominator
}

internal fun BezierConnection.calculateMinRadius(): Double {
	var minRadius = Double.POSITIVE_INFINITY
	val p1 = starts.first.toFlat()
	val p2 = starts.second.toFlat()
	val q1 = axes.first.toFlat().mul(handleLength).add(p1)
	val q2 = axes.second.toFlat().mul(handleLength).add(p2)
	
	val derivative = Vector3d()
	val derivative2 = Vector3d()
	for(segment in 0..segmentCount) {
		val t = getSegmentT(segment).toDouble()
		
		derivative.set(0.0)
		derivative.fma(-3 * t * t + 6 * t - 3, p1)
		derivative.fma(9 * t * t - 12 * t + 3, q1)
		derivative.fma(-9 * t * t + 6 * t, q2)
		derivative.fma(3 * t * t, p2)
		
		derivative2.set(0.0)
		derivative2.fma(-6 * t + 6, p1)
		derivative2.fma(18 * t - 12, q1)
		derivative2.fma(-18 * t + 6, q2)
		derivative2.fma(6 * t, p2)
		
		val derivativeLength = derivative.length().pow3()
		
		derivative.cross(derivative2)
		val denominator = derivative.length()
		if(denominator < 1e-6) continue
		
		val radius = derivativeLength / denominator
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


// this is not precise split, as BezierConnection uses same handleLength for each end
fun BezierConnection.splitIntoTwo(t: Double): Couple<BezierConnection> {
	val p1 = starts.first
	val p2 = starts.second
	val q1 = p1 + axes.first * handleLength
	val q2 = p2 + axes.second * handleLength
	val bePosition = BlockPos.containing(VecHelper.bezier(p1, p2, q1, q2, t.toFloat()))
	val position = Vec3.atBottomCenterOf(bePosition)
	val axis = derivative(t).normalize()
	val faceNormal = if(normals.first closeTo normals.second) {
		normals.first
	} else {
		VecHelper.slerp(t.toFloat(), normals.first, normals.second)
	}
	val normal = axis.cross(faceNormal.cross(axis)).normalize()
	
	val first = BezierConnection(
		Couple.create(bePositions.first, bePosition),
		Couple.create(starts.first, position),
		Couple.create(axes.first, -axis),
		Couple.create(normals.first, normal),
		true,
		hasGirder,
		material,
	)
	first.copiedFrom(this)
	
	val second = BezierConnection(
		Couple.create(bePosition, bePositions.second),
		Couple.create(position, starts.second),
		Couple.create(axis, axes.second),
		Couple.create(normal, normals.second),
		true,
		hasGirder,
		material,
	)
	second.copiedFrom(this)
	
	return Couple.create(first, second)
}
