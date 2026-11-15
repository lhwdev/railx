package com.lhwdev.minecraft.railx.advancedRoller

import com.lhwdev.minecraft.railx.utils.math.*
import com.lhwdev.minecraft.utils.vectors.addMul
import com.lhwdev.minecraft.utils.vectors.setMul
import com.simibubi.create.content.contraptions.actors.roller.PaveTask
import com.simibubi.create.content.contraptions.actors.roller.TrackPaverV2
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.track.BezierConnection
import it.unimi.dsi.fastutil.longs.Long2DoubleMaps
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap
import net.minecraft.util.Mth
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


object TrackPaverV3 {
	fun pave(task: PaveTask, graph: TrackGraph, edge: TrackEdge, from: Double, to: Double) {
		if(edge.isTurn) {
			paveCurve(task, edge.turn, from, to)
			return
		}
		
		return TrackPaverV2.pave(task, graph, edge, from, to)
	}
	
	fun paveCurve(task: PaveTask, bc: BezierConnection, from: Double, to: Double) {
		val yLevels = Long2DoubleOpenHashMap()
		yLevels.defaultReturnValue(Double.POSITIVE_INFINITY)
		
		val origin = bc.bePositions.first
		
		val radius = -task.horizontalInterval.first
		val r1 = radius - .575
		val r2 = radius + .575
		
		val bezier = Bezier3d.from(bc)
		val normals = if(bc.normals.first closeTo bc.normals.second) {
			Vector3dLerp(bc.normals.first, bc.normals.second)
		} else {
			Vector3dSlerp(bc.normals.first, bc.normals.second)
		}
		
		val position = Vector3d()
		val normal = Vector3d()
		val hNormal = Vector3d()
		val a = Vector3d()
		val b = Vector3d()
		val c = Vector3d()
		val d = Vector3d()
		
		val triangle = HeightTriangle()
		val triangle1 = HeightTriangle()
		
		val segCount = bc.segmentCount
		val lut = bc.stepLUT
		val fromT = min(from, to) / bc.length
		val toT = max(from, to) / bc.length
		
		for(i in 0..<segCount) {
			val t = (i * lut[i] / segCount).toDouble()
			if(t > toT) continue
			
			val t1 = if((i + 1) == segCount) 1.0 else ((i + 1) * lut[(i + 1)] / segCount).toDouble()
			if(t1 < fromT) continue
			
			bezier.positionTo(t, dest = position)
			normals.interpolateTo(t, dest = normal)
			bezier.derivativeTo(t, dest = hNormal).normalizeOrZero()
				.cross(normal).normalizeOrZero()
			
			position.addMul(normal, -1.175)
			a.setMul(hNormal, r1).add(position)
			d.setMul(hNormal, r2).add(position)
			
			bezier.positionTo(t1, dest = position)
			normals.interpolateTo(t1, dest = normal)
			bezier.derivativeTo(t1, dest = hNormal).normalizeOrZero()
				.cross(normal).normalizeOrZero()
			
			position.addMul(normal, -1.175)
			b.setMul(hNormal, r1).add(position)
			c.setMul(hNormal, r2).add(position)
			
			triangle.set(a, b, c)
			triangle1.set(a, c, d)
			val minX = Mth.floor(min(min(a.x, b.x), min(c.x, d.x)))
			val maxX = Mth.floor(max(max(a.x, b.x), max(c.x, d.x)))
			val minZ = Mth.floor(min(min(a.z, b.z), min(c.z, d.z)))
			val maxZ = Mth.floor(max(max(a.z, b.z), max(c.z, d.z)))
			
			var scanX = minX
			while(scanX <= maxX) {
				var scanZ = minZ
				while(scanZ <= maxZ) {
					val x = scanX + .5
					val z = scanZ + .5
					var y = triangle.heightAt(x, z)
					if(y.isNaN()) y = triangle1.heightAt(x, z)
					if(!y.isNaN()) {
						val key = (scanX.toLong() shl 32) or (scanZ.toLong() and 0xffffffffL)
						if(y < yLevels.get(key)) yLevels.put(key, y)
					}
					scanZ++
				}
				scanX++
			}
		}
		
		val entries = Long2DoubleMaps.fastIterator(yLevels)
		while(entries.hasNext()) {
			val entry = entries.next()
			val x = (entry.longKey shr 32).toInt()
			val z = entry.longKey.toInt()
			val yValue = entry.doubleValue
			val floor = Mth.floor(yValue)
			
			val offset = floor((yValue - floor) * 16) / 16
			task.put(x + origin.x, z + origin.z, floor + origin.y + offset.toFloat())
		}
	}
	
	
	private class HeightTriangle {
		private var ax = 0.0
		private var ay = 0.0
		private var az = 0.0
		private var bx = 0.0
		private var bz = 0.0
		private var cx = 0.0
		private var cz = 0.0
		private var by = 0.0
		private var cy = 0.0
		private var inverse = Double.NaN
		
		fun set(a: Vector3d, b: Vector3d, c: Vector3d) {
			ax = a.x
			ay = a.y
			az = a.z
			bx = b.x - ax
			bz = b.z - az
			cx = c.x - ax
			cz = c.z - az
			by = b.y - ay
			cy = c.y - ay
			val determinant = bx * cz - bz * cx
			inverse = if(abs(determinant) > 1e-12) 1.0 / determinant else Double.NaN
		}
		
		fun heightAt(x: Double, z: Double): Double {
			val dx = x - ax
			val dz = z - az
			val u = (dx * cz - dz * cx) * inverse
			val v = (bx * dz - bz * dx) * inverse
			
			if(!(u >= -1e-9 && v >= -1e-9 && u + v <= 1.0 + 1e-9)) return Double.NaN
			return ay + u * by + v * cy
		}
	}
}
