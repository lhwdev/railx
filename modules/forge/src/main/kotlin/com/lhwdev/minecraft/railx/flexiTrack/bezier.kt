package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.content.trains.track.BezierConnection
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.times


internal fun BezierConnection.calculateMinimumRadius(): Double {
	// curvature = | f' cross f'' | over | f' |^3
	// see https://doi.org/10.1016/S0377-0427(00)00529-X (Curvature extrema of planar parametric polynomial cubic curves)
	
	
	TODO()
}


private fun bezierDerivative(p1: Vec3, p2: Vec3, q1: Vec3, q2: Vec3, t: Double) =
	p1 * (-3 * t * t + 6 * t - 3) + q1 * (9 * t * t - 12 * t + 3) + q2 * (-9 * t * t + 6 * t) + p2 * (3 * t * t)

private fun bezierDerivative2(p1: Vec3, p2: Vec3, q1: Vec3, q2: Vec3, t: Double) =
	p1 * (-6 * t + 6) + q1 * (18 * t - 12) + q2 * (-18 * t + 6) + p2 * (6 * t)
