package com.lhwdev.minecraft.railx.advancedRoller

import com.simibubi.create.content.contraptions.actors.roller.PaveTask
import com.simibubi.create.content.contraptions.actors.roller.TrackPaverV2
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.track.BezierConnection
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import net.createmod.catnip.data.Pair
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.floor


object TrackPaverV3 {
	fun pave(task: PaveTask, graph: TrackGraph, edge: TrackEdge, from: Double, to: Double) {
		if(edge.isTurn) {
			paveCurve(task, edge.turn, from, to)
			return
		}
		
		return TrackPaverV2.pave(task, graph, edge, from, to)
	}
	
	fun paveCurve(task: PaveTask, bc: BezierConnection, from: Double, to: Double) {
		val yLevels = Object2DoubleOpenHashMap<Pair<Int, Int>>()
		val tLevels = Object2DoubleOpenHashMap<Pair<Int, Int>>()
		
		val bePosition = bc.bePositions.first
		val radius = -task.horizontalInterval.first
		val r1 = radius - .575
		val r2 = radius + .575
		
		val handleLength = bc.handleLength
		val start = bc.starts.first
			.subtract(Vec3.atLowerCornerOf(bePosition))
			.add(0.0, (3 / 16f).toDouble(), 0.0)
		val end = bc.starts.second
			.subtract(Vec3.atLowerCornerOf(bePosition))
			.add(0.0, (3 / 16f).toDouble(), 0.0)
		val startHandle = bc.axes.first
			.scale(handleLength)
			.add(start)
		val endHandle = bc.axes.second
			.scale(handleLength)
			.add(end)
		val startNormal = bc.normals.first
		val endNormal = bc.normals.second
		
		val segCount = bc.segmentCount
		val lut = bc.stepLUT
		val localFrom = from / bc.length
		val localTo = to / bc.length
		
		for(i in 0..<segCount) {
			val t = i * lut[i] / segCount
			val t1 = if((i + 1) == segCount) 1f else (i + 1) * lut[(i + 1)] / segCount
			
			if(t1 < localFrom) continue
			if(t > localTo) continue
			
			var vt = VecHelper.bezier(start, end, startHandle, endHandle, t)
			val vNormal = if(startNormal == endNormal) startNormal else VecHelper.slerp(t, startNormal, endNormal)
			val hNormal = vNormal.cross(
				VecHelper.bezierDerivative(start, end, startHandle, endHandle, t)
					.normalize()
			).normalize()
			vt = vt.add(vNormal.scale(-1.175))
			
			var vt1 = VecHelper.bezier(start, end, startHandle, endHandle, t1)
			val vNormal1 = if(startNormal == endNormal) startNormal else VecHelper.slerp(t1, startNormal, endNormal)
			val hNormal1 = vNormal1.cross(
				VecHelper.bezierDerivative(start, end, startHandle, endHandle, t1)
					.normalize()
			).normalize()
			vt1 = vt1.add(vNormal1.scale(-1.175))
			
			val a3 = vt.add(hNormal.scale(r2))
			val b3 = vt1.add(hNormal1.scale(r2))
			val c3 = vt1.add(hNormal1.scale(r1))
			val d3 = vt.add(hNormal.scale(r1))
			
			val a = Vec2(a3.x.toFloat(), a3.z.toFloat())
			val b = Vec2(b3.x.toFloat(), b3.z.toFloat())
			val c = Vec2(c3.x.toFloat(), c3.z.toFloat())
			val d = Vec2(d3.x.toFloat(), d3.z.toFloat())
			
			val aabb = AABB(a3, b3).minmax(AABB(c3, d3))
			
			val y = vt.add(vt1).y / 2f
			var scanX = Mth.floor(aabb.minX)
			while(scanX <= aabb.maxX) {
				var scanZ = Mth.floor(aabb.minZ)
				while(scanZ <= aabb.maxZ) {
					val p = Vec2(scanX + .5f, scanZ + .5f)
					if(!isInTriangle(a, b, c, p) && !isInTriangle(a, c, d, p)) {
						scanZ++
						continue
					}
					
					val key = Pair.of(scanX, scanZ)
					if(key !in yLevels || yLevels.getDouble(key) > y) {
						yLevels[key] = y
						tLevels[key] = (t + t1) / 2.0
					}
					scanZ++
				}
				scanX++
			}
		}
		
		for(entry in yLevels.object2DoubleEntrySet()) {
			val yValue = entry.doubleValue
			val floor = Mth.floor(yValue)
			val targetPos = BlockPos(entry.key.first, floor, entry.key.second)
				.offset(bePosition)
			
			val offset = floor((yValue - floor) * 16) / 16
			task.put(targetPos.x, targetPos.z, targetPos.y + offset.toFloat())
		}
	}
	
	private fun isInTriangle(a: Vec2, b: Vec2, c: Vec2, p: Vec2): Boolean {
		val pcx = p.x - c.x
		val pcy = p.y - c.y
		val cbx = c.x - b.x
		val bcy = b.y - c.y
		val d = bcy * (a.x - c.x) + cbx * (a.y - c.y)
		val s = bcy * pcx + cbx * pcy
		val t = (c.y - a.y) * pcx + (a.x - c.x) * pcy
		return if(d < 0) s <= 0 && t <= 0 && s + t >= d else s >= 0 && t >= 0 && s + t <= d
	}
}
