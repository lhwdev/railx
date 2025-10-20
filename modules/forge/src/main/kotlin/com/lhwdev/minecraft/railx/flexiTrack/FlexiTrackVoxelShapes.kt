package com.lhwdev.minecraft.railx.flexiTrack

import net.createmod.catnip.math.VecHelper
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Quaterniond
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVector3d
import kotlin.math.max
import kotlin.math.min


val VoxelCenter = Vec3(8.0, 8.0, 8.0)


private fun Vec3.normalAsRotation(): Quaterniond = Quaterniond()
	.rotationZ(Mth.atan2(y, x))
	.rotateY(Mth.atan2(z, x))
	.rotateX(Mth.atan2(z, y))

object FlexiTrackVoxelShapes {
	val collision = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0)
	
	val base = Block.box(0.0, 0.0, -14.0, 16.0, 4.0, 30.0)
	// val base = Block.box(0.0, 0.0, -28.0, 32.0, 8.0, 60.0)
	
	private val knownCache = arrayOfNulls<VoxelShape>(FlexiDirection.Known.DivisionCount)
	
	fun known(direction: FlexiDirection.Known): VoxelShape {
		val ordinal = direction.ordinal
		return knownCache[ordinal] ?: createKnown(direction).also { knownCache[ordinal] = it }
	}
	
	fun of(direction: FlexiDirection, cache: FlexiState.AxisCache? = null): VoxelShape = when(direction) {
		FlexiDirection.Zero -> Shapes.empty()
		is FlexiDirection.Known -> known(direction)
		else -> createShape(direction, cache)
	}
	
	fun of(shape: FlexiShape, cache: List<FlexiState.AxisCache>? = null): VoxelShape =
		shape.axes.foldIndexed(Shapes.empty()) { index, acc, axis ->
			Shapes.or(acc, of(axis, cache = cache?.get(index)))
		}
	
	fun of(state: FlexiState): VoxelShape =
		of(state.shape, cache = state.shapeCache)
	
	private fun createKnown(direction: FlexiDirection.Known): VoxelShape {
		var result = Shapes.empty()
		val rotation = direction.angleDegree
		
		base.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
			var v1 = Vec3(x1, y1, z1).scale(16.30)
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
	
	
	fun createShape(direction: FlexiDirection, cache: FlexiState.AxisCache? = null): VoxelShape =
		rotatedCopy(base, cache?.rotationValueDouble ?: direction.normal.normalAsRotation())
	
	private fun rotatedCopy(shape: VoxelShape, rotation: Quaterniond): VoxelShape {
		if(rotation == Vec3.ZERO) return shape
		
		var result = Shapes.empty()
		
		shape.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
			var v1 = Vec3(x1, y1, z1).scale(16.0)
				.subtract(VoxelCenter)
			var v2 = Vec3(x2, y2, z2).scale(16.0)
				.subtract(VoxelCenter)
			
			v1 = rotation.transform(v1.toVector3d()).toVec3().add(VoxelCenter)
			v2 = rotation.transform(v2.toVector3d()).toVec3().add(VoxelCenter)
			
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
