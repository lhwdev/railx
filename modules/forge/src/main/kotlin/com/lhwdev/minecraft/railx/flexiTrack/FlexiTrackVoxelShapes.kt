package com.lhwdev.minecraft.railx.flexiTrack

import net.createmod.catnip.math.VecHelper
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.max
import kotlin.math.min


val VoxelCenter = Vec3(8.0, 8.0, 8.0)


private fun Vec3.normalAsRotation(): Vec3 = Vec3(
	Mth.atan2(z, y),
	Mth.atan2(z, x),
	Mth.atan2(y, x),
)

object FlexiTrackVoxelShapes {
	val collision = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0)
	
	val base = Block.box(0.0, 0.0, -14.0, 16.0, 4.0, 30.0)
	// val base = Block.box(0.0, 0.0, -28.0, 32.0, 8.0, 60.0)
	
	private val knownCache = arrayOfNulls<VoxelShape>(FlexiDirection.Known.DivisionCount)
	
	fun known(direction: FlexiDirection.Known): VoxelShape {
		val ordinal = direction.ordinal
		return knownCache[ordinal] ?: createKnown(direction).also { knownCache[ordinal] = it }
	}
	
	fun of(direction: FlexiDirection): VoxelShape = when(direction) {
		FlexiDirection.Zero -> Shapes.empty()
		is FlexiDirection.Known -> known(direction)
		else -> createShape(direction.normal.normalAsRotation())
	}
	
	fun of(shape: FlexiShape): VoxelShape =
		shape.axes.fold(Shapes.empty()) { acc, axis -> Shapes.or(acc, of(axis)) }
	
	fun of(state: FlexiState): VoxelShape =
		of(state.shape)
	
	private fun createKnown(direction: FlexiDirection.Known): VoxelShape {
		var result = Shapes.empty()
		val rotation = direction.angleDegree
		
		base.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
			var v1 = Vec3(x1, y1, z1).scale(16.0)
				.subtract(VoxelCenter)
			var v2 = Vec3(x2, y2, z2).scale(16.0)
				.subtract(VoxelCenter)
			
			v1 = VecHelper.rotate(v1, rotation, Direction.Axis.Y)
				.add(VoxelCenter)
			v2 = VecHelper.rotate(v2, rotation, Direction.Axis.Y)
				.add(VoxelCenter)
			
			val rotated = blockBox(v1, v2)
			result = Shapes.or(result, rotated)
		}
		
		return result
	}
	
	
	fun createShape(rotation: Vec3): VoxelShape =
		rotatedCopy(base, rotation)
	
	private fun rotatedCopy(shape: VoxelShape, rotation: Vec3): VoxelShape {
		if(rotation == Vec3.ZERO) return shape
		
		var result = Shapes.empty()
		
		shape.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
			var v1 = Vec3(x1, y1, z1).scale(16.0)
				.subtract(VoxelCenter)
			var v2 = Vec3(x2, y2, z2).scale(16.0)
				.subtract(VoxelCenter)
			
			v1 = VecHelper.rotate(v1, rotation.x, Direction.Axis.X)
			v1 = VecHelper.rotate(v1, rotation.y, Direction.Axis.Y)
			v1 = VecHelper.rotate(v1, rotation.z, Direction.Axis.Z)
				.add(VoxelCenter)
			
			v2 = VecHelper.rotate(v2, rotation.x, Direction.Axis.X)
			v2 = VecHelper.rotate(v2, rotation.y, Direction.Axis.Y)
			v2 = VecHelper.rotate(v2, rotation.z, Direction.Axis.Z)
				.add(VoxelCenter)
			
			val rotated = blockBox(v1, v2)
			result = Shapes.or(result, rotated)
		}
		
		return result
	}
	
	private fun blockBox(v1: Vec3, v2: Vec3): VoxelShape = Block.box(
		min(v1.x, v2.x),
		min(v1.y, v2.y),
		min(v1.z, v2.z),
		max(v1.x, v2.x),
		max(v1.y, v2.y),
		max(v1.z, v2.z)
	)
}
