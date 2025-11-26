@file:JvmName("FakeTracks")

package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackBlockEntity
import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.FakeTrackBlock
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.LongArrayList
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import com.simibubi.create.AllBlocks as CreateBlocks


abstract class LongFakeTrackState {
	
	
	protected abstract fun placeFakeTracks(level: Level, segments: IntRange, remove: Boolean)
	
	
}


private const val MiddlePlacementBaseGap = 16

class LongMiddleOnlyTrackState(private val bc: BezierConnection) : LongFakeTrackState() {
	private val middles = bc.rasterizeMiddlesOrdered(offset = bc.bePositions.first.offset(0, 1, 0))
	
	
	private val removePrevious = RailXConfig.Server.middleTrack.removePrevious.get()
	
	private val pos = BlockPos.MutableBlockPos()
	
	override fun placeFakeTracks(level: Level, segments: IntRange, remove: Boolean) {
		for(index in segments) {
			if(index % 16 != 0) continue
			pos.set(middles.getLong(index))
			val state = level.getBlockState(pos)
			if(remove) removeFakeTrack(level, state) else placeFakeTrack(level, state)
		}
	}
	
	private fun placeFakeTrack(level: Level, stateAtPos: BlockState) {
		val fluidState = stateAtPos.fluidState
		if(!fluidState.isEmpty && !fluidState.isSourceOfType(Fluids.WATER)) return
		
		val pos = pos
		var middlePlaced = false
		if(AllBlocks.MiddleTrack.has(stateAtPos)) {
			val previous = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
			if(previous != null && previous.connections.none { it.bePositions == bc.bePositions }) {
				previous.updateConnections(previous.connections.plus<BezierConnection>(bc))
				middlePlaced = true
			}
		}
		if(!middlePlaced && stateAtPos.canBeReplaced()) {
			level.setBlock(
				pos,
				ProperWaterloggedBlock.withWater(level, AllBlocks.MiddleTrack.defaultState, pos),
				3
			)
			(level.getBlockEntity(pos) as? MiddleTrackBlockEntity)?.updateConnections(listOf(bc))
		}
		
		FakeTrackBlock.keepAlive(level, pos)
	}
	
	private fun removeFakeTrack(level: Level, stateAtPos: BlockState) {
		val pos = pos
		val fakePresent = CreateBlocks.FAKE_TRACK.has(stateAtPos)
		val middlePresent = AllBlocks.MiddleTrack.has(stateAtPos)
		
		if(fakePresent) level.removeBlock(pos, false)
		if(middlePresent) {
			if(removePrevious) {
				level.removeBlock(pos, false)
				return
			}
			val middle = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
			if(middle == null) {
				level.removeBlock(pos, false)
				return
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
	}
}

class LongFakeTrackStateImpl(private val bc: BezierConnection) : LongFakeTrackState() {
	private val blocks = bc.rasterizeOrdered(offset = bc.bePositions.first.offset(0, 1, 0))
	
	private val middleIndices: IntArrayList = IntArrayList().also { indices ->
		val maxGap = RailXConfig.Server.middleTrack.placeGap.get()
		var chunkX = BlockPos.getX(blocks.getLong(0)) shr 4
		var chunkZ = BlockPos.getZ(blocks.getLong(0)) shr 4
		for(index in blocks.indices step MiddlePlacementBaseGap) {
			val pos = blocks.getLong(index)
			val newChunkX = BlockPos.getX(pos) shr 4
			val newChunkZ = BlockPos.getZ(pos) shr 4
			val placeMiddle = abs(newChunkX - chunkX) >= maxGap || abs(newChunkZ - chunkZ) >= maxGap
			if(placeMiddle) {
				indices.add(index)
				chunkX = newChunkX
				chunkZ = newChunkZ
			}
		}
	}
	
	private val placeMiddle = RailXConfig.Server.middleTrack.enablePlacing.get()
	private val removePrevious = RailXConfig.Server.middleTrack.removePrevious.get()
	
	private val pos = BlockPos.MutableBlockPos()
	
	
	override fun placeFakeTracks(level: Level, segments: IntRange, remove: Boolean) {
		for(index in segments) {
			pos.set(blocks.getLong(index))
			val state = level.getBlockState(pos)
			if(remove) removeFakeTrack(level, state) else placeFakeTrack(level, index, state)
		}
	}
	
	private fun placeFakeTrack(level: Level, index: Int, stateAtPos: BlockState) {
		val fluidState = stateAtPos.fluidState
		if(!fluidState.isEmpty && !fluidState.isSourceOfType(Fluids.WATER)) return
		
		val pos = pos
		var middlePlaced = false
		if(index % MiddlePlacementBaseGap == 0 && placeMiddle && middleIndices.contains(index)) {
			if(AllBlocks.MiddleTrack.has(stateAtPos)) {
				val previous = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
				if(previous != null && previous.connections.none { it.bePositions == bc.bePositions }) {
					previous.updateConnections(previous.connections.plus<BezierConnection>(bc))
					middlePlaced = true
				}
			}
			if(!middlePlaced && stateAtPos.canBeReplaced()) {
				level.setBlock(
					pos,
					ProperWaterloggedBlock.withWater(level, AllBlocks.MiddleTrack.defaultState, pos),
					3
				)
				(level.getBlockEntity(pos) as? MiddleTrackBlockEntity)?.updateConnections(listOf(bc))
			}
		} else if(!CreateBlocks.FAKE_TRACK.has(stateAtPos) && !AllBlocks.MiddleTrack.has(stateAtPos) && stateAtPos.canBeReplaced()) {
			level.setBlock(
				pos,
				ProperWaterloggedBlock.withWater(level, CreateBlocks.FAKE_TRACK.defaultState, pos),
				3
			)
		}
		
		FakeTrackBlock.keepAlive(level, pos)
	}
	
	private fun removeFakeTrack(level: Level, stateAtPos: BlockState) {
		val pos = pos
		val fakePresent = CreateBlocks.FAKE_TRACK.has(stateAtPos)
		val middlePresent = AllBlocks.MiddleTrack.has(stateAtPos)
		
		if(fakePresent) level.removeBlock(pos, false)
		if(middlePresent) {
			if(removePrevious) {
				level.removeBlock(pos, false)
				return
			}
			val middle = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
			if(middle == null) {
				level.removeBlock(pos, false)
				return
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
	}
}


fun manageFakeTracksAlong(be: TrackBlockEntity, bc: BezierConnection, remove: Boolean) {
	val level = be.level!!
	val pos = BlockPos.MutableBlockPos()
	val blocks = bc.rasterizeOrdered(offset = bc.bePositions.first.offset(0, 1, 0))
	
	if(blocks.isEmpty()) return
	var chunkX = BlockPos.getX(blocks.getLong(0)) shr 4
	var chunkZ = BlockPos.getZ(blocks.getLong(0)) shr 4
	val maxGap = RailXConfig.Server.middleTrack.placeGap.get()
	val placeMiddle = RailXConfig.Server.middleTrack.enablePlacing.get()
	val removePrevious = RailXConfig.Server.middleTrack.removePrevious.get()
	
	
	var middlesPlaced = 0
	
	for(index in blocks.indices) {
		pos.set(blocks.getLong(index))
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
		
		var middlePlaced = false
		if(placeMiddle && middle) {
			if(middlePresent) {
				val previous = level.getBlockEntity(pos) as? MiddleTrackBlockEntity
				if(previous != null && previous.connections.none { it.bePositions == bc.bePositions }) {
					previous.updateConnections(previous.connections.plus<BezierConnection>(bc))
					middlePlaced = true
				}
			}
			if(!middlePlaced && stateAtPos.canBeReplaced()) {
				level.setBlock(pos, ProperWaterloggedBlock.withWater(level, AllBlocks.MiddleTrack.defaultState, pos), 3)
				(level.getBlockEntity(pos) as? MiddleTrackBlockEntity)?.updateConnections(listOf(bc))
				middlePlaced = true
			}
			
			if(middlePlaced) {
				chunkX = newChunkX
				chunkZ = newChunkZ
				middlesPlaced++
			}
		} else if(!fakePresent && !middlePresent && stateAtPos.canBeReplaced()) {
			level.setBlock(pos, ProperWaterloggedBlock.withWater(level, CreateBlocks.FAKE_TRACK.defaultState, pos), 3)
		}
		
		FakeTrackBlock.keepAlive(level, pos)
	}
}


fun BezierConnection.rasterizeMiddlesOrdered(offset: Vec3i): LongArrayList {
	val result = LongArrayList()
	val tePosition = bePositions.first
	val end1 = starts.first
		.subtract(Vec3.atLowerCornerOf(tePosition))
		.add(0.0, 3.0 / 16, 0.0)
	val end2 = starts.second
		.subtract(Vec3.atLowerCornerOf(tePosition))
		.add(0.0, 3.0 / 16, 0.0)
	val axis1 = axes.first
	val axis2 = axes.second
	
	val handleLength = handleLength
	val finish1 = axis1.scale(handleLength)
		.add(end1)
	val finish2 = axis2.scale(handleLength)
		.add(end2)
	
	val faceNormal1 = normals.first
	val faceNormal2 = normals.second
	
	val segCount = segmentCount
	val lut = stepLUT
	
	val maxGap = RailXConfig.Server.middleTrack.placeGap.get()
	
	var chunkX: Int
	var chunkZ: Int
	run {
		val t = 0.5f / segCount
		val point = VecHelper.bezier(end1, end2, finish1, finish2, t)
		val derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
			.normalize()
		val faceNormal =
			if(faceNormal1 == faceNormal2) faceNormal1 else VecHelper.slerp(t, faceNormal1, faceNormal2)
		val normal = faceNormal.cross(derivative)
			.normalize()
		val below = point.add(faceNormal.scale(-.25))
		val rail1 = below.add(normal.scale(.05))
		val rail2 = below.subtract(normal.scale(.05))
		val railMiddle = rail1.add(rail2).scale(.5)
		chunkX = Mth.floor(railMiddle.x) + offset.x shr 4
		chunkZ = Mth.floor(railMiddle.z) + offset.z shr 4
	}
	
	for(i in MiddlePlacementBaseGap..<segCount step MiddlePlacementBaseGap) {
		val t = Mth.clamp((i + 0.5f) * lut[i] / segCount, 0f, 1f)
		val point = VecHelper.bezier(end1, end2, finish1, finish2, t)
		val derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
			.normalize()
		val faceNormal =
			if(faceNormal1 == faceNormal2) faceNormal1 else VecHelper.slerp(t, faceNormal1, faceNormal2)
		val normal = faceNormal.cross(derivative)
			.normalize()
		val below = point.add(faceNormal.scale(-.25))
		val rail1 = below.add(normal.scale(.05))
		val rail2 = below.subtract(normal.scale(.05))
		val railMiddle = rail1.add(rail2).scale(.5)
		
		val x = Mth.floor(railMiddle.x) + offset.x
		val y = Mth.floor(railMiddle.y) + offset.y
		val z = Mth.floor(railMiddle.z) + offset.z
		val pos = BlockPos.asLong(x, y, z)
		
		val newChunkX = x shr 4
		val newChunkZ = z shr 4
		val placeMiddle = abs(newChunkX - chunkX) >= maxGap || abs(newChunkZ - chunkZ) >= maxGap
		if(!placeMiddle) continue
		
		result.add(pos)
		chunkX = newChunkX
		chunkZ = newChunkZ
	}
	return result
}

fun BezierConnection.rasterizeOrdered(offset: Vec3i): LongArrayList {
	val result = LongArrayList()
	val tePosition = bePositions.first
	val end1 = starts.first
		.subtract(Vec3.atLowerCornerOf(tePosition))
		.add(0.0, 3.0 / 16, 0.0)
	val end2 = starts.second
		.subtract(Vec3.atLowerCornerOf(tePosition))
		.add(0.0, 3.0 / 16, 0.0)
	val axis1 = axes.first
	val axis2 = axes.second
	
	val handleLength = handleLength
	val finish1 = axis1.scale(handleLength)
		.add(end1)
	val finish2 = axis2.scale(handleLength)
		.add(end2)
	
	val faceNormal1 = normals.first
	val faceNormal2 = normals.second
	
	val segCount = segmentCount
	val lut = stepLUT
	
	val center = end1.add(end2).scale(0.5)
	
	for(i in 0..<segCount) {
		val t = Mth.clamp((i + 0.5f) * lut[i] / segCount, 0f, 1f)
		val point = VecHelper.bezier(end1, end2, finish1, finish2, t)
		val derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
			.normalize()
		val faceNormal =
			if(faceNormal1 == faceNormal2) faceNormal1 else VecHelper.slerp(t, faceNormal1, faceNormal2)
		val normal = faceNormal.cross(derivative)
			.normalize()
		val below = point.add(faceNormal.scale(-.25))
		val rail1 = below.add(normal.scale(.05))
		val rail2 = below.subtract(normal.scale(.05))
		val railMiddle = rail1.add(rail2).scale(.5)
		
		val pos = BlockPos.asLong(
			Mth.floor(railMiddle.x) + offset.x,
			Mth.floor(railMiddle.y) + offset.y,
			Mth.floor(railMiddle.z) + offset.z,
		)
		result.add(pos)
		if(i >= 3 && result.size >= i) { // Remove obsolete pixels
			val prev = result.getLong(i - 1)
			val prev2 = result.getLong(i - 2)
			val prev3 = result.getLong(i - 3)
			val doubledViaPrev = isLineDoubled(prev2, prev, pos)
			val doubledViaPrev2 = isLineDoubled(prev3, prev2, prev)
			val prevCloser = diff(prev, center) > diff(prev2, center)
			
			if(doubledViaPrev2 && (!doubledViaPrev || !prevCloser)) {
				result.removeLong(i - 2)
				continue
			} else if(doubledViaPrev && doubledViaPrev2 && prevCloser) {
				result.removeLong(i - 1)
				continue
			}
		}
	}
	return result
}

private fun diff(from: Long, to: Vec3): Double {
	return to.distanceToSqr(BlockPos.getX(from) + 0.5, to.y, BlockPos.getZ(from) + 0.5)
}

private fun isLineDoubled(from: Long, via: Long, to: Long): Boolean {
	val diff1x = BlockPos.getX(via) - BlockPos.getZ(from)
	val diff1z = BlockPos.getX(via) - BlockPos.getZ(from)
	val diff2x = BlockPos.getX(to) - BlockPos.getZ(via)
	val diff2z = BlockPos.getX(to) - BlockPos.getZ(via)
	return abs(diff1x) + abs(diff1z) == 1 && abs(diff2x) + abs(diff2z) == 1 && diff1x != diff2x && diff1z != diff2z
}
