package com.lhwdev.minecraft.railx.utils.math

import com.lhwdev.minecraft.utils.vectors.setLerp
import com.lhwdev.minecraft.utils.vectors.setMul
import com.simibubi.create.content.trains.track.BezierConnection
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d


/**
 * Optimized for repeated invocation.
 */
class Bezier3d(val start: Vec3, val end: Vec3, val q1: Vec3, val q2: Vec3) {
	companion object {
		fun from(bc: BezierConnection): Bezier3d {
			val origin = Vec3.atLowerCornerOf(bc.bePositions.first)
			val handleLength = bc.handleLength
			
			val start = bc.starts.first
				.subtract(origin)
				.add(0.0, (3 / 16f).toDouble(), 0.0)
			val end = bc.starts.second
				.subtract(origin)
				.add(0.0, (3 / 16f).toDouble(), 0.0)
			val startHandle = bc.axes.first
				.scale(handleLength)
				.add(start)
			val endHandle = bc.axes.second
				.scale(handleLength)
				.add(end)
			
			return Bezier3d(start, end, startHandle, endHandle)
		}
	}
	
	
	private val t1 = Vector3d()
	private val t2 = Vector3d()
	private val t3 = Vector3d()
	
	fun positionTo(t: Double, dest: Vector3d): Vector3d {
		t1.setLerp(start, q1, t)
		t2.setLerp(q1, q2, t)
		t3.setLerp(q2, end, t)
		t1.lerp(t2, t)
		t2.lerp(t3, t)
		return dest.setLerp(t1, t2, t)
	}
	
	fun derivativeTo(t: Double, dest: Vector3d): Vector3d {
		dest.setMul(start, -3 * t * t + 6 * t - 3)
		dest.add(t1.setMul(q1, 9 * t * t - 12 * t + 3))
		dest.add(t1.setMul(q2, -9 * t * t + 6 * t))
		return dest.add(t1.setMul(end, 3 * t * t))
	}
}
