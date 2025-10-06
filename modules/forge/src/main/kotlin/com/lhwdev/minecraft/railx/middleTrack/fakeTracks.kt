@file:JvmName("FakeTracks")

package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.FakeTrackBlock
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import com.simibubi.create.AllBlocks as CreateBlocks


class MiddleFakeTrackState(private var bc: BezierConnection) {
	private var blocks = bc.rasterizeOrdered()
	
	fun updateConnection(newBc: BezierConnection) {
		if(bc == newBc) {
			bc = newBc
			return
		}
		bc = newBc
		blocks = bc.rasterizeOrdered()
	}
	
	fun placeFakeTracks(level: Level) {
		TODO()
	}
}


fun manageFakeTracksAlong(be: TrackBlockEntity, bc: BezierConnection, remove: Boolean) {
	val level = be.level!!
	val blocks = bc.rasterizeOrdered().map {
		it.offset(bc.bePositions.first).above(1)
	}
	
	if(blocks.isEmpty()) return
	var chunkX = blocks.first().x shr 4
	var chunkZ = blocks.first().z shr 4
	val maxGap = RailXConfig.Server.middleTrack.placeGap.asInt
	val placeMiddle = RailXConfig.Server.middleTrack.enablePlacing.asBoolean
	val removePrevious = RailXConfig.Server.middleTrack.removePrevious.asBoolean
	
	for(pos in blocks) {
		val stateAtPos = level.getBlockState(pos)
		val fakePresent = CreateBlocks.FAKE_TRACK.has(stateAtPos)
		val middlePresent = AllBlocks.MiddleTrack.has(stateAtPos)
		
		if(remove) {
			if(fakePresent) level.removeBlock(pos, false)
			if(middlePresent) {
				if(removePrevious) {
					level.removeBlock(pos, false)
					continue
				}
				val middle = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
				if(middle == null) {
					level.removeBlock(pos, false)
					continue
				}
				val previous = middle.connections.indexOfFirst { it.bePositions == bc.bePositions }
				if(previous != -1) {
					val connections = middle.connections.toMutableList().also { it.removeAt(previous) }
					if(connections.isEmpty()) {
						level.removeBlock(pos, false)
					} else {
						middle.updateConnections(connections)
					}
				}
			}
			continue
		}
		
		val fluidState = stateAtPos.fluidState
		if(!fluidState.isEmpty && !fluidState.isSourceOfType(Fluids.WATER)) continue
		
		val newChunkX = pos.x shr 4
		val newChunkZ = pos.z shr 4
		val middle = abs(newChunkX - chunkX) >= maxGap || abs(newChunkZ - chunkZ) >= maxGap
		
		if(placeMiddle && middle) {
			if(middlePresent) {
				val previous = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
				if(previous == null) {
					continue
				}
				if(previous.connections.none { it.bePositions == bc.bePositions })
					previous.updateConnections(previous.connections.plus<BezierConnection>(bc))
			} else if(stateAtPos.canBeReplaced()) {
				level.setBlock(pos, ProperWaterloggedBlock.withWater(level, AllBlocks.MiddleTrack.defaultState, pos), 3)
				(level.getBlockEntity(pos) as? MiddleTrackBlockEntity)?.updateConnections(listOf(bc))
			} else {
				continue
			}
			
			chunkX = newChunkX
			chunkZ = newChunkZ
		} else if(!fakePresent && !middlePresent && stateAtPos.canBeReplaced()) {
			level.setBlock(pos, ProperWaterloggedBlock.withWater(level, CreateBlocks.FAKE_TRACK.defaultState, pos), 3)
		}
		
		FakeTrackBlock.keepAlive(level, pos)
	}
}


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
