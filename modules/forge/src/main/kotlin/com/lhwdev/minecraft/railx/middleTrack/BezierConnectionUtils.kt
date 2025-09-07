package com.lhwdev.minecraft.railx.middleTrack

import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs


object BezierConnectionUtils {
	fun BezierConnection.rasterizeOrdered(): List<BlockPos> {
		val result = mutableListOf<BlockPos>()
		val tePosition = bePositions.getFirst()
		val end1 = starts.getFirst()
			.subtract(Vec3.atLowerCornerOf(tePosition))
			.add(0.0, 3.0 / 16, 0.0)
		val end2 = starts.getSecond()
			.subtract(Vec3.atLowerCornerOf(tePosition))
			.add(0.0, 3.0 / 16, 0.0)
		val axis1 = axes.getFirst()
		val axis2 = axes.getSecond()
		
		val handleLength = getHandleLength()
		val finish1 = axis1.scale(handleLength)
			.add(end1)
		val finish2 = axis2.scale(handleLength)
			.add(end2)
		
		val faceNormal1 = normals.getFirst()
		val faceNormal2 = normals.getSecond()
		
		val segCount = getSegmentCount()
		val lut = getStepLUT()
		val samples = ArrayList<Vec3>(segCount)
		
		for(i in 0..<segCount) {
			val t = Mth.clamp((i + 0.5f) * lut[i] / segCount, 0f, 1f)
			val result = VecHelper.bezier(end1, end2, finish1, finish2, t)
			val derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
				.normalize()
			val faceNormal =
				if(faceNormal1 == faceNormal2) faceNormal1 else VecHelper.slerp(t, faceNormal1, faceNormal2)
			val normal = faceNormal.cross(derivative)
				.normalize()
			val below = result.add(faceNormal.scale(-.25))
			val rail1 = below.add(normal.scale(.05))
			val rail2 = below.subtract(normal.scale(.05))
			val railMiddle = rail1.add(rail2)
				.scale(.5)
			samples += railMiddle
		}
		
		val center = end1.add(end2)
			.scale(0.5)
		
		
		for(i in 0..<segCount) {
			val railMiddle = samples[i]
			val pos = BlockPos.containing(railMiddle)
			result += pos
			
			if(i >= 3 && result.size >= i) { // Remove obsolete pixels
				val prev = result[i - 1]
				val prev2 = result[i - 2]
				val prev3 = result[i - 3]
				val doubledViaPrev = isLineDoubled(prev2, prev, pos)
				val doubledViaPrev2 = isLineDoubled(prev3, prev2, prev)
				val prevCloser = diff(prev, center) > diff(prev2, center)
				
				if(doubledViaPrev2 && (!doubledViaPrev || !prevCloser)) {
					result.removeAt(i - 2)
					continue
				} else if(doubledViaPrev && doubledViaPrev2 && prevCloser) {
					result.removeAt(i - 1)
					continue
				}
			}
		}
		return result
	}
	
	private fun diff(pFrom: BlockPos, to: Vec3): Double {
		return to.distanceToSqr(pFrom.x + 0.5, to.y, pFrom.z + 0.5)
	}
	
	private fun isLineDoubled(
		pFrom: BlockPos,
		pVia: BlockPos,
		pTo: BlockPos,
	): Boolean {
		val diff1x = pVia.x - pFrom.z
		val diff1z = pVia.x - pFrom.z
		val diff2x = pTo.x - pVia.z
		val diff2z = pTo.x - pVia.z
		return abs(diff1x) + abs(diff1z) == 1 && abs(diff2x) + abs(diff2z) == 1 && diff1x != diff2x && diff1z != diff2z
	}
}