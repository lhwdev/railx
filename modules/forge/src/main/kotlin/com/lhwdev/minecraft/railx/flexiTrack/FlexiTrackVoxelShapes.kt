@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.rotate.direction
import com.lhwdev.minecraft.railx.flexiTrack.rotate.rotationValue
import com.lhwdev.minecraft.railx.utils.similarTo
import it.unimi.dsi.fastutil.doubles.AbstractDoubleList
import it.unimi.dsi.fastutil.doubles.DoubleList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.DiscreteVoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Quaterniond
import org.joml.Vector3d
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVector3d
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.unaryMinus
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


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
		of(shape.axis1, cache?.get(0))
	// shape.axes.foldIndexed(Shapes.empty()) { index, acc, axis ->
	// 	Shapes.or(acc, of(axis, cache = cache?.get(index)))
	// }
	
	fun of(state: FlexiState): VoxelShape =
		of(state.shape, cache = state.shapeCache)
	
	private fun createKnown(direction: FlexiDirection.Known): VoxelShape {
		val rotation = Quaterniond().rotationY(direction.direction)
		val info = FlatTrackVoxelInfo(rotation)
		return FlatTrackVoxelShape(DiscreteTrackVoxelShape(info))
	}
	
	
	fun createShape(direction: FlexiDirection, cache: FlexiState.AxisCache? = null): VoxelShape {
		val rotation = cache?.rotationValueDouble ?: direction.rotationValue()
		return if(direction is FlexiDirection.Flat) {
			val info = FlatTrackVoxelInfo(rotation)
			FlatTrackVoxelShape(DiscreteTrackVoxelShape(info))
		} else {
			val info = TrackVoxelInfo(rotation)
			TrackVoxelShape(DiscreteTrackVoxelShape(info))
		}
	}
	
	private fun blockBox(v1: Vec3, v2: Vec3): VoxelShape = Block.box(
		min(v1.x, v2.x),
		min(v1.y, v2.y),
		min(v1.z, v2.z),
		max(v1.x, v2.x),
		max(v1.y, v2.y),
		max(v1.z, v2.z),
	)
}


private const val Step = 0.125

private const val CenterX = 0.5
private const val CenterY = 0.125
private const val CenterZ = 0.5
private val Center = Vector3d(CenterX, CenterY, CenterZ)

// private val BoxCentered = AABB(-0.5, -0.125, -1.375, 0.5, 0.125, 1.375)
private const val WidthX = 0.5 // half size
private const val WidthY = 0.125
private const val WidthZ = 1.375
// private val BoxSizeHalfPow = Vec3(0.5 * 0.5, 0.125 * 0.125, 1.375 * 1.375)

private open class TrackVoxelInfo(rotation: Quaterniond) {
	private val dx = rotation.transformUnit(Vector3d(1.0, 0.0, 0.0))
	private val dy = rotation.transformUnit(Vector3d(0.0, 1.0, 0.0))
	private val dz = rotation.transformUnit(Vector3d(0.0, 0.0, 1.0))
	
	fun isInsideBoxCentered(pos: Vector3d): Boolean =
		abs(pos.dot(dx)) <= WidthX &&
			abs(pos.dot(dy)) <= WidthY &&
			abs(pos.dot(dz)) <= WidthZ
	
	protected open fun createBound(range: AABB) = TrackVoxelBound(
		minX = Mth.floor(range.minX / Step - 0.5),
		minY = Mth.floor(range.minY / Step - 0.5),
		minZ = Mth.floor(range.minZ / Step - 0.5),
		maxX = Mth.ceil(range.maxX / Step + 0.5), // range.maxN is exclusive
		maxY = Mth.ceil(range.maxY / Step + 0.5),
		maxZ = Mth.ceil(range.maxZ / Step + 0.5),
	)
	
	
	val bound: TrackVoxelBound
	protected val lookup: BitSet
	
	init {
		val dxScaled = Vector3d(dx).mul(WidthX)
		val dyScaled = Vector3d(dy).mul(WidthY)
		val dzScaled = Vector3d(dz).mul(WidthZ)
		val buffer = Vector3d()
		var range = buffer.set(dxScaled).add(dyScaled).add(dzScaled).toVec3().let { AABB(it, -it) }
		buffer.set(dyScaled).sub(dxScaled).add(dzScaled)
		range = range.growToInclude(buffer)
		range = range.growToInclude(buffer.mul(-1.0))
		buffer.set(dxScaled).sub(dyScaled).add(dzScaled)
		range = range.growToInclude(buffer)
		range = range.growToInclude(buffer.mul(-1.0))
		buffer.set(dxScaled).add(dyScaled).sub(dzScaled)
		range = range.growToInclude(buffer)
		range = range.growToInclude(buffer.mul(-1.0))
		
		
		val rangeOffset = range.move(CenterX, CenterY, CenterZ)
		val bound = createBound(rangeOffset)
		val lookup = BitSet(bound.sizeX * bound.sizeY * bound.sizeZ)
		val currentPos = buffer
		for(y in 0..<bound.sizeY) {
			for(z in 0..<bound.sizeZ) {
				for(x in 0..<bound.sizeX) {
					val index = x + (z + y * bound.sizeY) * bound.sizeX // ... which just increases by 1
					currentPos.set(
						(x + bound.minX + 0.5) * Step - CenterX,
						(y + bound.minY + 0.5) * Step - CenterY,
						(z + bound.minZ + 0.5) * Step - CenterZ,
					)
					lookup[index] = isInsideBoxCentered(currentPos)
				}
			}
		}
		
		this.bound = bound
		this.lookup = lookup
		if(true);
	}
	
	
	open fun lookup(x: Int, y: Int, z: Int): Boolean =
		lookup[bound.lookupIndex(x, y, z)]
	
	fun clip(from: Vec3, to: Vec3, pos: BlockPos): BlockHitResult? {
		// if(range.clip(from, to)) // AABB.clip is not that faster than this... I think; need some benchmark? maybe?
		
		val offset = pos.toVector3d() + Center
		val delta = to.toVector3d().sub(from.x, from.y, from.z)
		val from = from.toVector3d().sub(offset)
		
		if(isInsideBoxCentered(from)) {
			return BlockHitResult(
				from.add(offset).toVec3(),
				Direction.getNearest(delta.mul(-1.0).toVec3()),
				pos,
				true
			)
		}
		
		val buffer = Vector3d()
		var bestT = Double.POSITIVE_INFINITY
		var bestNormal = dx
		var bestNormalSign = 1
		
		val deltaOnX = delta.dot(dx)
		if(!(deltaOnX similarTo 0.0)) { // TODO: does not take care of edge-cases (literally)
			val fromOnX = from.dot(dx)
			
			// 1. on -x face
			var t = (-WidthX - fromOnX) / deltaOnX
			if(t < bestT && t in 0.0..1.0) {
				val pos = buffer.set(delta).mul(t).add(from)
				if(abs(pos.dot(dy)) <= WidthY && abs(pos.dot(dz)) <= WidthZ) {
					bestT = t
					bestNormal = dx
					bestNormalSign = -1
				}
			}
			
			// 2. on +x face
			t = (WidthX - fromOnX) / deltaOnX
			if(t < bestT && t in 0.0..1.0) {
				val pos = buffer.set(delta).mul(t).add(from)
				if(abs(pos.dot(dy)) <= WidthY && abs(pos.dot(dz)) <= WidthZ) {
					bestT = t
					bestNormal = dx
					bestNormalSign = 1
				}
			}
		}
		
		val deltaOnY = delta.dot(dy)
		if(!(deltaOnY similarTo 0.0)) {
			val fromOnY = from.dot(dy)
			
			// 3. on -y face
			var t = (-WidthY - fromOnY) / deltaOnY
			if(t < bestT && t in 0.0..1.0) {
				val pos = buffer.set(delta).mul(t).add(from)
				if(abs(pos.dot(dx)) <= WidthX && abs(pos.dot(dz)) <= WidthZ) {
					bestT = t
					bestNormal = dy
					bestNormalSign = -1
				}
			}
			
			// 4. on +y face
			t = (WidthY - fromOnY) / deltaOnY
			if(t < bestT && t in 0.0..1.0) {
				val pos = buffer.set(delta).mul(t).add(from)
				if(abs(pos.dot(dx)) <= WidthX && abs(pos.dot(dz)) <= WidthZ) {
					bestT = t
					bestNormal = dy
					bestNormalSign = 1
				}
			}
		}
		val deltaOnZ = delta.dot(dz)
		if(!(deltaOnZ similarTo 0.0)) {
			val fromOnZ = from.dot(dz)
			
			// 5. on -z face
			var t = (-WidthZ - fromOnZ) / deltaOnZ
			if(t < bestT && t in 0.0..1.0) {
				val pos = buffer.set(delta).mul(t).add(from)
				if(abs(pos.dot(dx)) <= WidthX && abs(pos.dot(dy)) <= WidthY) {
					bestT = t
					bestNormal = dz
					bestNormalSign = -1
				}
			}
			
			// 6. on +z face
			t = (WidthZ - fromOnZ) / deltaOnZ
			if(t < bestT && t in 0.0..1.0) {
				val pos = buffer.set(delta).mul(t).add(from)
				if(abs(pos.dot(dx)) <= WidthX && abs(pos.dot(dy)) <= WidthY) {
					bestT = t
					bestNormal = dz
					bestNormalSign = 1
				}
			}
		}
		
		return if(bestT == Double.POSITIVE_INFINITY) {
			null
		} else {
			val location = delta.mul(bestT).add(from).add(offset) // << mutation
			val normal = buffer.set(bestNormal).mul(bestNormalSign.toDouble())
			BlockHitResult(
				location.toVec3(),
				Direction.getNearest(normal.x, normal.y, normal.z),
				pos,
				false
			)
		}
	}
	
	fun dumpLookupToString(): String = buildString {
		val y = (bound.minY + bound.maxY - 1) / 2
		for(x in 0..<bound.sizeX) {
			for(z in 0..<bound.sizeZ) {
				val data = lookup(x, y, z)
				append(if(data) '1' else '0')
			}
			append('\n')
		}
	}
}

private class FlatTrackVoxelInfo(rotation: Quaterniond) : TrackVoxelInfo(rotation) {
	override fun createBound(range: AABB) = TrackVoxelBound(
		minX = Mth.floor(range.minX / Step - 0.5),
		minY = 0,
		minZ = Mth.floor(range.minZ / Step - 0.5),
		maxX = Mth.ceil(range.maxX / Step + 0.5),
		maxY = 1,
		maxZ = Mth.ceil(range.maxZ / Step + 0.5),
	)
	
	override fun lookup(x: Int, y: Int, z: Int): Boolean =
		lookup[bound.lookupIndex(x, 0, z)]
}


private class TrackVoxelBound(
	@JvmField val minX: Int,
	@JvmField val minY: Int,
	@JvmField val minZ: Int,
	@JvmField val maxX: Int,
	@JvmField val maxY: Int,
	@JvmField val maxZ: Int,
) {
	@JvmField val sizeX = maxX - minX
	@JvmField val sizeY = maxY - minY
	@JvmField val sizeZ = maxZ - minZ
	
	inline fun lookupIndex(x: Int, y: Int, z: Int) =
		x + (z + y * sizeY) * sizeX
}

private class VoxelRange(val min: Int, override val size: Int) : AbstractDoubleList() {
	override fun getDouble(index: Int): Double = (min + index) * Step
}

private class TrackVoxelShape(shape: DiscreteTrackVoxelShape) : VoxelShape(shape) {
	private val info = shape.info
	private val listX = VoxelRange(info.bound.minX, info.bound.sizeX + 1)
	private val listY = VoxelRange(info.bound.minY, info.bound.sizeY + 1)
	private val listZ = VoxelRange(info.bound.minZ, info.bound.sizeZ + 1)
	
	override fun getCoords(axis: Direction.Axis): DoubleList = when(axis) {
		Direction.Axis.X -> listX
		Direction.Axis.Y -> listY
		Direction.Axis.Z -> listZ
	}
	
	override fun clip(startVec: Vec3, endVec: Vec3, pos: BlockPos): BlockHitResult? =
		info.clip(startVec, endVec, pos)
}

private class FlatTrackVoxelShape(shape: DiscreteTrackVoxelShape) : VoxelShape(shape) {
	private object YRange : AbstractDoubleList() {
		override val size: Int
			get() = 2
		
		override fun getDouble(index: Int): Double = when(index) {
			0 -> 0.0
			1 -> WidthY * 2
			else -> throw NoWhenBranchMatchedException()
		}
	}
	
	private val info = shape.info
	private val listX = VoxelRange(info.bound.minX, info.bound.sizeX + 1)
	private val listY = YRange
	private val listZ = VoxelRange(info.bound.minZ, info.bound.sizeZ + 1)
	
	override fun getCoords(axis: Direction.Axis): DoubleList = when(axis) {
		Direction.Axis.X -> listX
		Direction.Axis.Y -> listY
		Direction.Axis.Z -> listZ
	}
	
	override fun clip(startVec: Vec3, endVec: Vec3, pos: BlockPos): BlockHitResult? =
		info.clip(startVec, endVec, pos)
}

private class DiscreteTrackVoxelShape(val info: TrackVoxelInfo) :
	DiscreteVoxelShape(info.bound.sizeX, info.bound.sizeY, info.bound.sizeZ) {
	override fun isFull(x: Int, y: Int, z: Int): Boolean =
		info.lookup(x, y, z)
	
	override fun fill(x: Int, y: Int, z: Int) = throw NotImplementedError()
	
	override fun firstFull(axis: Direction.Axis): Int = 0
	
	override fun lastFull(axis: Direction.Axis): Int = when(axis) {
		Direction.Axis.X -> info.bound.sizeX
		Direction.Axis.Y -> info.bound.sizeY
		Direction.Axis.Z -> info.bound.sizeZ
	}
}


private fun AABB.growToInclude(vec: Vector3d): AABB = AABB(
	min(minX, vec.x),
	min(minY, vec.y),
	min(minZ, vec.z),
	max(maxX, vec.x),
	max(maxY, vec.y),
	max(maxZ, vec.z),
)
