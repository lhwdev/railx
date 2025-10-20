package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.utils.*
import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import kotlin.math.PI


val FlexiShape.isJunction
	get() = axes.size > 1

sealed interface FlexiShape {
	val axes: List<FlexiDirection>
	val axesCount: Int get() = axes.size
	val axis1: FlexiDirection get() = axes[0]
	val axis2: FlexiDirection? get() = axes.getOrNull(1)
	
	val tangents: List<Vec3> get() = axes.map { it.tangent }
	val normal: Vec3
	
	fun mirror(by: Mirror): FlexiShape
	
	fun rotate(by: Rotation): FlexiShape
	fun rotateKnown(by: Int): FlexiShape
	
	fun write(): CompoundTag
	
	fun insert(direction: FlexiDirection): FlexiShape
	
	
	object Empty : FlexiShape {
		override val axes: List<FlexiDirection>
			get() = listOf(FlexiDirection.Zero)
		override val axesCount: Int
			get() = 1
		override val axis1: FlexiDirection
			get() = FlexiDirection.Zero
		override val normal: Vec3
			get() = Vec3(0.0, 1.0, 0.0)
		
		override fun mirror(by: Mirror): Empty = this
		override fun rotate(by: Rotation): Empty = this
		override fun rotateKnown(by: Int): Empty = this
		
		override fun insert(direction: FlexiDirection): Single =
			Single(direction)
		
		override fun write(): CompoundTag = CompoundTag { tag ->
			tag.putByte("Type", 0x0)
		}
		
		override fun toString(): String = "FlexiShape.Empty"
	}
	
	class Single(val axis: FlexiDirection) : FlexiShape {
		companion object {
			fun read(tag: CompoundTag): Single = Single(
				axis = tag.get("Axis").let {
					if(it is NumericTag) {
						FlexiDirection.Known.readInt(it)
					} else {
						FlexiDirection.read(it as CompoundTag)
					}
				}
			)
		}
		
		override val axes: List<FlexiDirection>
			get() = listOf(axis)
		override val axesCount: Int
			get() = 1
		override val axis1: FlexiDirection
			get() = axis
		override val axis2: FlexiDirection?
			get() = null
		
		override val tangents: List<Vec3>
			get() = listOf(axis.tangent)
		
		val tangent: Vec3
			get() = axis.tangent
		
		override val normal: Vec3
			get() = axis.normal
		
		override fun mirror(by: Mirror): Single =
			Single(axis.mirror(by))
		
		override fun rotate(by: Rotation): Single =
			Single(axis.rotate(by))
		
		override fun rotateKnown(by: Int): Single =
			Single(axis.rotateKnown(by))
		
		override fun insert(direction: FlexiDirection): FlexiShape {
			if(!checkInsert(direction)) return this
			return Impl(listOf(axis, direction))
		}
		
		override fun write(): CompoundTag = CompoundTag { tag ->
			tag.putByte("Type", 0x1)
			tag.put("Axis", if(axis is FlexiDirection.Known) axis.writeInt() else axis.write())
		}
		
		override fun toString(): String = "FlexiShape.Single($axis)"
	}
	
	class NormalizedImpl(
		val flatAxes: List<FlexiDirection.Flat>,
		override val normal: Vec3,
		override val axes: List<FlexiDirection.Normalized> = flatAxes.map { it.applyNormal(normal) },
	) : FlexiShape {
		companion object {
			fun read(tag: CompoundTag): NormalizedImpl = NormalizedImpl(
				flatAxes = readDirectionList(tag.get("FlatAxes") as ListTag).requireAllInstanceOf(),
				normal = VecHelper.readNBT(tag.getList("Normal", Tag.TAG_DOUBLE.toInt())),
			)
		}
		
		init {
			require(flatAxes.isNotEmpty()) { "flatAxes is empty" }
			require(flatAxes.size == axes.size) { "flatAxes.size != axes.size" }
		}
		
		override fun mirror(by: Mirror): NormalizedImpl {
			val flat = flatAxes.map { it.mirror(by) }
			val normal = by.mirror(normal)
			return NormalizedImpl(flat, normal, axes.mapIndexed { index, axis ->
				FlexiDirection.NormalizedImpl(flat[index], normal, by.mirror(axis.tangent))
			})
		}
		
		override fun rotate(by: Rotation): NormalizedImpl {
			val flat = flatAxes.map { it.rotate(by) }
			val normal = by.rotate(normal)
			return NormalizedImpl(flat, normal, axes.mapIndexed { index, axis ->
				FlexiDirection.NormalizedImpl(flat[index], normal, by.rotate(axis.tangent))
			})
		}
		
		override fun rotateKnown(by: Int): NormalizedImpl {
			val angle = by.toFloat() / FlexiDirection.Known.DivisionCount * PI.toFloat()
			val flat = flatAxes.map { it.rotateKnown(by) }
			val normal = normal.yRot(angle)
			return NormalizedImpl(flat, normal, axes.mapIndexed { index, axis ->
				FlexiDirection.NormalizedImpl(flat[index], normal, axis.tangent.yRot(angle))
			})
		}
		
		override fun insert(direction: FlexiDirection): FlexiShape {
			if(!checkInsert(direction)) return this
			return Impl(axes + direction)
		}
		
		override fun write(): CompoundTag = CompoundTag { tag ->
			tag.putByte("Type", 0x10)
			tag.put("FlatAxes", writeDirectionList(flatAxes))
			tag.put("Normal", VecHelper.writeNBT(normal))
		}
		
		override fun toString(): String = "FlexiShape.Impl($axes)"
	}
	
	class Impl(override val axes: List<FlexiDirection>) : FlexiShape {
		companion object {
			fun read(tag: CompoundTag): Impl =
				Impl(axes = readDirectionList(tag.getList("Axes", Tag.TAG_COMPOUND.toInt())))
		}
		
		init {
			require(axes.isNotEmpty()) { "axes is empty" }
		}
		
		override val normal: Vec3
			get() = axes[0].normal
		
		override fun mirror(by: Mirror): Impl = Impl(axes.map { it.mirror(by) })
		override fun rotate(by: Rotation): Impl = Impl(axes.map { it.rotate(by) })
		override fun rotateKnown(by: Int): Impl = Impl(axes.map { it.rotateKnown(by) })
		
		override fun write(): CompoundTag = CompoundTag { tag ->
			tag.putByte("Type", 0x11)
			tag.put("Axes", writeDirectionList(axes))
		}
		
		override fun insert(direction: FlexiDirection): FlexiShape {
			if(!checkInsert(direction)) return this
			return Impl(axes + direction)
		}
	}
	
	companion object {
		fun from(axes: List<FlexiDirection.Flat>): FlexiShape = when(axes.size) {
			0 -> Empty
			1 -> Single(axes[0])
			else -> NormalizedImpl(axes, normal = Empty.normal)
		}
		
		fun read(tag: CompoundTag): FlexiShape = when(tag.getByte("Type")) {
			0x0.toByte() -> Empty
			0x1.toByte() -> Single.read(tag)
			0x10.toByte() -> NormalizedImpl.read(tag)
			0x11.toByte() -> Impl.read(tag)
			else -> TODO()
		}
	}
}

private fun FlexiShape.checkInsert(direction: FlexiDirection): Boolean {
	if((normal - direction.normal).lengthSqr() > 1.0e-10) {
		throw IllegalArgumentException("direction.normal != normal")
	}
	return axes.none { it closeToUnsigned direction }
}

private fun readDirectionList(tag: ListTag) = tag.let { axesTag ->
	if(axesTag.first() is NumericTag) {
		axesTag.map { FlexiDirection.Known.readInt(it as NumericTag) }
	} else {
		axesTag.map { FlexiDirection.read(it as CompoundTag) }
	}
}

private fun writeDirectionList(axes: List<FlexiDirection>): ListTag = ListTag().also { list ->
	val result = if(axes.all { it is FlexiDirection.Known }) {
		axes.map { (it as FlexiDirection.Known).writeInt() }
	} else {
		axes.map { it.write() }
	}
	list.addAll(result)
}


inline fun FlexiShape.map(block: (axis: FlexiDirection) -> FlexiDirection): FlexiShape {
	return when(this) {
		FlexiShape.Empty -> FlexiShape.Empty
		is FlexiShape.Impl -> FlexiShape.Impl(axes.map(block))
		is FlexiShape.NormalizedImpl -> {
			var axes = axes.map(block)
			axes = axes.asAllInstanceOf<FlexiDirection.Normalized>()
				?: return FlexiShape.Impl(axes)
			if(axes.isEmpty()) return FlexiShape.Empty
			val flatAxes = flatAxes.map(block).asAllInstanceOf<FlexiDirection.Flat>()
				?: return FlexiShape.Impl(axes)
			FlexiShape.NormalizedImpl(flatAxes, axes.first().normal, axes)
		}
		
		is FlexiShape.Single -> FlexiShape.Single(block(axis))
	}
}
