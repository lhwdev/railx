@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.rotate.direction
import com.lhwdev.minecraft.railx.flexiTrack.rotate.rotationValue
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.toVec3
import com.lhwdev.minecraft.utils.vectors.toVector3d
import com.lhwdev.minecraft.utils.vectors.unaryMinus
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
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


open class FlexiTrackVoxelShapes {
	companion object {
		val Standard: VoxelShape = Block.box(0.0, 0.0, -14.0, 16.0, 4.0, 30.0)
		
		val StandardShapes: FlexiTrackVoxelShapes = FlexiTrackVoxelShapes()
		
		
		fun fromSingleBoxShape(shape: VoxelShape): FlexiTrackVoxelShapes = SingleBoxShapes(shape)
	}
	
	open val base: VoxelShape
		get() = Standard
	
	open val spec: BoxSpec
		get() = BoxSpec.Standard
	
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
	
	// TODO: support multiple directions? is this needed?
	fun of(shape: FlexiShape, cache: List<FlexiState.AxisCache>? = null): VoxelShape =
		of(shape.axis1, cache?.get(0))
	
	fun of(state: FlexiState): VoxelShape =
		of(state.shape, cache = state.shapeCache)
	
	fun createKnown(direction: FlexiDirection.Known): VoxelShape {
		val rotation = Quaterniond().rotationY(direction.direction)
		return createFlatShape(rotation)
	}
	
	
	fun createShape(direction: FlexiDirection, cache: FlexiState.AxisCache? = null): VoxelShape {
		val rotation = cache?.rotationValueDouble ?: direction.rotationValue()
		return if(direction is FlexiDirection.Flat) {
			createFlatShape(rotation)
		} else {
			createNormalShape(rotation)
		}
	}
	
	protected open fun createFlatShape(rotation: Quaterniond): VoxelShape {
		val box = FlatBoxShape(spec, rotation)
		return FlatTrackVoxelShape(box)
	}
	
	protected open fun createNormalShape(rotation: Quaterniond): VoxelShape {
		val info = BoxShape(spec, rotation)
		return BoxSetVoxelShapeImpl(info)
	}
	
	
	class SingleBoxShapes(override val base: VoxelShape) : FlexiTrackVoxelShapes() {
		override val spec: BoxSpec
		
		init {
			var spec: BoxSpec? = null
			base.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
				if(spec != null) throw IllegalArgumentException("provided box is not single")
				spec = BoxSpec(
					halfSizeX = (x2 - x1) / 2,
					halfSizeY = (y2 - y1) / 2,
					halfSizeZ = (z2 - z1) / 2,
					centerX = (x2 + x1) / 2,
					centerY = (y2 + y1) / 2,
					centerZ = (z2 + z1) / 2,
				)
			}
			if(spec == null) throw IllegalArgumentException()
			this.spec = spec
		}
	}
	
	
	// NOTE: I, as a poor innocent student, wrote this algorithm alone. Please let me know if this has some error or any
	//       ways to be improved.
	
	
	class BoxSpec(
		val halfSizeX: Double,
		val halfSizeY: Double,
		val halfSizeZ: Double,
		val centerX: Double,
		val centerY: Double,
		val centerZ: Double,
	) {
		val center = Vector3d(centerX, centerY, centerZ)
		
		companion object {
			const val Step = 0.125
			
			val Standard: BoxSpec = BoxSpec(
				halfSizeX = 0.5,
				halfSizeY = 0.125,
				halfSizeZ = 1.375,
				centerX = 0.5,
				centerY = 0.125,
				centerZ = 0.5,
			)
		}
	}
	
	class BoxBound(
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
		
		fun union(other: BoxBound): BoxBound = BoxBound(
			minX = min(minX, other.minX),
			minY = min(minY, other.minY),
			minZ = min(minZ, other.minZ),
			maxX = max(maxX, other.maxX),
			maxY = max(maxY, other.maxY),
			maxZ = max(maxZ, other.maxZ),
		)
	}
	
	
	open class BoxShape(val spec: BoxSpec, rotation: Quaterniond) {
		private val dx = rotation.transformUnit(Vector3d(1.0, 0.0, 0.0))
		private val dy = rotation.transformUnit(Vector3d(0.0, 1.0, 0.0))
		private val dz = rotation.transformUnit(Vector3d(0.0, 0.0, 1.0))
		
		protected open fun createBound(range: AABB) = BoxBound(
			minX = Mth.floor(range.minX / BoxSpec.Step - 0.5),
			minY = Mth.floor(range.minY / BoxSpec.Step - 0.5),
			minZ = Mth.floor(range.minZ / BoxSpec.Step - 0.5),
			maxX = Mth.ceil(range.maxX / BoxSpec.Step + 0.5), // range.maxN is exclusive
			maxY = Mth.ceil(range.maxY / BoxSpec.Step + 0.5),
			maxZ = Mth.ceil(range.maxZ / BoxSpec.Step + 0.5),
		)
		
		
		val bound: BoxBound
		
		init {
			val dxScaled = Vector3d(dx).mul(spec.halfSizeX)
			val dyScaled = Vector3d(dy).mul(spec.halfSizeY)
			val dzScaled = Vector3d(dz).mul(spec.halfSizeZ)
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
			
			val rangeOffset = range.move(spec.centerX, spec.centerY, spec.centerZ)
			bound = createBound(rangeOffset)
		}
		
		
		fun isInsideBoxCentered(pos: Vector3d): Boolean =
			abs(pos.dot(dx)) <= spec.halfSizeX &&
				abs(pos.dot(dy)) <= spec.halfSizeY &&
				abs(pos.dot(dz)) <= spec.halfSizeZ
		
		fun updateLookup(lookup: BitSet, startX: Int, startY: Int, startZ: Int) {
			val currentPos = Vector3d()
			for(y in 0..<bound.sizeY) {
				for(z in 0..<bound.sizeZ) {
					for(x in 0..<bound.sizeX) {
						currentPos.set(
							(x + bound.minX + 0.5) * BoxSpec.Step - spec.centerX,
							(y + bound.minY + 0.5) * BoxSpec.Step - spec.centerY,
							(z + bound.minZ + 0.5) * BoxSpec.Step - spec.centerZ,
						)
						if(isInsideBoxCentered(currentPos)) {
							val index = (x + startX) + ((z + startZ) + (y + startY) * bound.sizeY) * bound.sizeX
							lookup.set(index)
						}
					}
				}
			}
		}
		
		
		open fun clip(from: Vec3, to: Vec3, pos: BlockPos): BlockHitResult? {
			// if(range.clip(from, to)) // AABB.clip is not that faster than this... I think; need some benchmark? maybe?
			
			val spec = spec
			val sizeX = spec.halfSizeX
			val sizeY = spec.halfSizeY
			val sizeZ = spec.halfSizeZ
			val offset = pos.toVector3d().add(spec.center)
			val delta = to.toVector3d().sub(from.x, from.y, from.z)
			val from = from.toVector3d().sub(offset)
			
			if(isInsideBoxCentered(from)) {
				return BlockHitResult(
					from.add(offset).toVec3(),
					Direction.getNearest(-delta.x, -delta.y, -delta.z),
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
				var t = (-sizeX - fromOnX) / deltaOnX
				if(t < bestT && t in 0.0..1.0) {
					val pos = buffer.set(delta).mul(t).add(from)
					if(abs(pos.dot(dy)) <= sizeY && abs(pos.dot(dz)) <= sizeZ) {
						bestT = t
						bestNormal = dx
						bestNormalSign = -1
					}
				}
				
				// 2. on +x face
				t = (sizeX - fromOnX) / deltaOnX
				if(t < bestT && t in 0.0..1.0) {
					val pos = buffer.set(delta).mul(t).add(from)
					if(abs(pos.dot(dy)) <= sizeY && abs(pos.dot(dz)) <= sizeZ) {
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
				var t = (-sizeY - fromOnY) / deltaOnY
				if(t < bestT && t in 0.0..1.0) {
					val pos = buffer.set(delta).mul(t).add(from)
					if(abs(pos.dot(dx)) <= sizeX && abs(pos.dot(dz)) <= sizeZ) {
						bestT = t
						bestNormal = dy
						bestNormalSign = -1
					}
				}
				
				// 4. on +y face
				t = (sizeY - fromOnY) / deltaOnY
				if(t < bestT && t in 0.0..1.0) {
					val pos = buffer.set(delta).mul(t).add(from)
					if(abs(pos.dot(dx)) <= sizeX && abs(pos.dot(dz)) <= sizeZ) {
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
				var t = (-sizeZ - fromOnZ) / deltaOnZ
				if(t < bestT && t in 0.0..1.0) {
					val pos = buffer.set(delta).mul(t).add(from)
					if(abs(pos.dot(dx)) <= sizeX && abs(pos.dot(dy)) <= sizeY) {
						bestT = t
						bestNormal = dz
						bestNormalSign = -1
					}
				}
				
				// 6. on +z face
				t = (sizeZ - fromOnZ) / deltaOnZ
				if(t < bestT && t in 0.0..1.0) {
					val pos = buffer.set(delta).mul(t).add(from)
					if(abs(pos.dot(dx)) <= sizeX && abs(pos.dot(dy)) <= sizeY) {
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
				BlockHitResult(location.toVec3(), Direction.getNearest(normal.x, normal.y, normal.z), pos, false)
			}
		}
		
	}
	
	class FlatBoxShape(spec: BoxSpec, rotation: Quaterniond) : BoxShape(spec, rotation) {
		override fun createBound(range: AABB) = BoxBound(
			minX = Mth.floor(range.minX / BoxSpec.Step - 0.5),
			minY = 0,
			minZ = Mth.floor(range.minZ / BoxSpec.Step - 0.5),
			maxX = Mth.ceil(range.maxX / BoxSpec.Step + 0.5),
			maxY = 1,
			maxZ = Mth.ceil(range.maxZ / BoxSpec.Step + 0.5),
		)
	}
	
	abstract class BoxShapeContainer {
		abstract val bound: BoxBound
		abstract fun lookup(x: Int, y: Int, z: Int): Boolean
	}
	
	class SingleBoxShapeContainer(val box: BoxShape) : BoxShapeContainer() {
		private val lookup: BitSet
		
		init {
			val bound = box.bound
			val lookup = BitSet(bound.sizeX * bound.sizeY * bound.sizeZ)
			box.updateLookup(lookup, startX = 0, startY = 0, startZ = 0)
			this.lookup = lookup
		}
		
		
		override val bound: BoxBound
			get() = box.bound
		
		override fun lookup(x: Int, y: Int, z: Int): Boolean =
			lookup[x + (z + y * bound.sizeY) * bound.sizeX]
	}
	
	abstract class BoxShapeContainerBase : BoxShapeContainer() {
		private val lookup: BitSet
		final override val bound: BoxBound
		
		protected abstract val boxes: Iterable<BoxShape>
		
		
		init {
			val boxIterator = boxes.iterator()
			var bound = boxIterator.next().bound
			while(boxIterator.hasNext()) bound = bound.union(boxIterator.next().bound)
			
			val lookup = BitSet(bound.sizeX * bound.sizeY * bound.sizeZ)
			for(box in boxes) {
				val boxBound = box.bound
				box.updateLookup(
					lookup,
					startX = boxBound.minX - bound.minX,
					startY = boxBound.minY - bound.minY,
					startZ = boxBound.minZ - bound.minZ,
				)
			}
			
			this.bound = bound
			this.lookup = lookup
		}
		
		
		override fun lookup(x: Int, y: Int, z: Int): Boolean =
			lookup[x + (z + y * bound.sizeY) * bound.sizeX]
	}
	
	
	abstract class BoxSetVoxelShape(boxes: BoxShapeContainer) :
		VoxelShape(DiscreteBoxSetVoxelShape(boxes)) {
		class VoxelRange(val min: Int, override val size: Int) : AbstractDoubleList() {
			override fun getDouble(index: Int): Double = (min + index) * BoxSpec.Step
		}
	}
	
	class DiscreteBoxSetVoxelShape(val boxes: BoxShapeContainer) :
		DiscreteVoxelShape(boxes.bound.sizeX, boxes.bound.sizeY, boxes.bound.sizeZ) {
		override fun isFull(x: Int, y: Int, z: Int): Boolean =
			boxes.lookup(x, y, z)
		
		override fun fill(x: Int, y: Int, z: Int) =
			error("this shape is readonly")
		
		override fun firstFull(axis: Direction.Axis): Int = 0
		
		override fun lastFull(axis: Direction.Axis): Int = when(axis) {
			Direction.Axis.X -> boxes.bound.sizeX
			Direction.Axis.Y -> boxes.bound.sizeY
			Direction.Axis.Z -> boxes.bound.sizeZ
		}
	}
	
	
	class BoxSetVoxelShapeImpl(boxes: BoxShapeContainer) : BoxSetVoxelShape(boxes) {
		constructor(box: BoxShape) : this(boxes = SingleBoxShapeContainer(box))
		
		private val listX = VoxelRange(boxes.bound.minX, boxes.bound.sizeX + 1)
		private val listY = VoxelRange(boxes.bound.minX, boxes.bound.sizeX + 1)
		private val listZ = VoxelRange(boxes.bound.minZ, boxes.bound.sizeZ + 1)
		
		override fun getCoords(axis: Direction.Axis): DoubleList = when(axis) {
			Direction.Axis.X -> listX
			Direction.Axis.Y -> listY
			Direction.Axis.Z -> listZ
		}
	}
	
	private class FlatTrackVoxelShape(val box: BoxShape) : BoxSetVoxelShape(SingleBoxShapeContainer(box)) {
		private inner class YRange : AbstractDoubleList() {
			override val size: Int
				get() = 2
			
			override fun getDouble(index: Int): Double = when(index) {
				0 -> 0.0
				1 -> box.spec.halfSizeY * 2
				else -> throw NoWhenBranchMatchedException()
			}
		}
		
		private val listX = VoxelRange(box.bound.minX, box.bound.sizeX + 1)
		private val listY = YRange()
		private val listZ = VoxelRange(box.bound.minZ, box.bound.sizeZ + 1)
		
		override fun getCoords(axis: Direction.Axis): DoubleList = when(axis) {
			Direction.Axis.X -> listX
			Direction.Axis.Y -> listY
			Direction.Axis.Z -> listZ
		}
		
		override fun clip(startVec: Vec3, endVec: Vec3, pos: BlockPos): BlockHitResult? =
			box.clip(startVec, endVec, pos)
		
	}
}


fun FlexiTrackVoxelShapes.BoxShapeContainer.dumpLookupToString(): String = buildString {
	val y = (bound.minY + bound.maxY - 1) / 2
	for(x in 0..<bound.sizeX) {
		for(z in 0..<bound.sizeZ) {
			val data = lookup(x, y, z)
			append(if(data) '1' else '0')
		}
		append('\n')
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
