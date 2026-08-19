package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan


class BezierConnectionRuntimeStub(bc: BezierConnection) {
	private val end1 = bc.starts.first
	private val end2 = bc.starts.second
	
	private val axis1 = bc.axes.first
	private val axis2 = bc.axes.second
	
	private var radius = 0.0
	private var handleLength = 0.0
	
	init {
		determineHandles(end1, end2, axis1, axis2)
	}
	
	private val finish1 = axis1.scale(handleLength).add(end1)
	private val finish2 = axis2.scale(handleLength).add(end2)
	
	val length: Double = computeLength(finish1, finish2, end1, end2, scanCount = 16)
	
	
	private fun determineHandles(end1: Vec3, end2: Vec3, axis1: Vec3, axis2: Vec3) {
		val cross1 = axis1.cross(Vec3(0.0, 1.0, 0.0))
		val cross2 = axis2.cross(Vec3(0.0, 1.0, 0.0))
		
		val a1 = Mth.atan2(-axis2.z, -axis2.x)
		val a2 = Mth.atan2(axis1.z, axis1.x)
		var angle = a1 - a2
		
		val circle = 2 * Mth.PI
		angle = (angle + circle) % circle
		if(abs(circle - angle) < abs(angle)) angle = circle - angle
		
		if(Mth.equal(angle, 0.0)) {
			val intersect = VecHelper.intersect(end1, end2, axis1, cross2, Direction.Axis.Y)
			if(intersect != null) {
				val t = abs(intersect[0])
				val u = abs(intersect[1])
				val min = min(t, u)
				val max = max(t, u)
				
				if(min > 1.2 && max / min > 1 && max / min < 3) {
					handleLength = (max - min)
					return
				}
			}
			
			handleLength = end2.distanceTo(end1) / 3
			return
		}
		
		val n = circle / angle
		val factor = 4 / 3.0 * tan(Math.PI / (2 * n))
		val intersect = VecHelper.intersect(end1, end2, cross1, cross2, Direction.Axis.Y)
		
		if(intersect == null) {
			handleLength = end2.distanceTo(end1) / 3
			return
		}
		
		radius = if(RailXConfig.Server.common.fixTrackBezierAsymmetry.isTrue) {
			abs(max(intersect[0], intersect[1]))
		} else {
			abs(intersect[1])
		}
		handleLength = radius * factor
		if(Mth.equal(handleLength, 0.0)) handleLength = 1.0
	}
	
	private fun computeLength(finish1: Vec3, finish2: Vec3, end1: Vec3, end2: Vec3, scanCount: Int): Double {
		var length = 0.0
		
		var previous = end1
		for(i in 0..scanCount) {
			val t = i / scanCount.toFloat()
			val result = VecHelper.bezier(end1, end2, finish1, finish2, t)
			length += result.distanceTo(previous)
			previous = result
		}
		return length
	}
}
