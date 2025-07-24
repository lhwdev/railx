package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.utils.mirror
import com.lhwdev.minecraft.railx.utils.rotate
import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.PI


val FlexiShape.isJunction
	get() = axes.size > 1

sealed interface FlexiShape {
	val axes: List<FlexiDirection>
	val axis1: FlexiDirection get() = axes[0]
	val axis2: FlexiDirection? get() = axes.getOrNull(1)
	
	val tangents: List<Vec3> get() = axes.map { it.tangent }
	val normal: Vec3
	
	fun mirror(by: Mirror): FlexiShape
	
	fun rotate(by: Rotation): FlexiShape
	fun rotateKnown(by: Int): FlexiShape
	
	fun write(): CompoundTag
	
	
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
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x0)
			tag.put("Axis", if(axis is FlexiDirection.Known) axis.writeInt() else axis.write())
		}
	}
	
	class Impl(
		val flatAxes: List<FlexiDirection.Flat>,
		override val normal: Vec3,
		override val axes: List<FlexiDirection.Normalized> = flatAxes.map { it.applyNormal(normal) },
	) : FlexiShape {
		companion object {
			fun read(tag: CompoundTag): Impl = Impl(
				flatAxes = (tag.getCompound("FlatAxes") as ListTag).let { axesTag ->
					if(axesTag.first() is NumericTag) {
						axesTag.map { FlexiDirection.Known.readInt(it as NumericTag) }
					} else {
						axesTag.map { FlexiDirection.read(it as CompoundTag) as FlexiDirection.Flat }
					}
				},
				normal = VecHelper.readNBT(tag.getList("Normal", Tag.TAG_DOUBLE.toInt())),
			)
		}
		
		override fun mirror(by: Mirror): Impl {
			val flat = flatAxes.map { it.mirror(by) }
			val normal = by.mirror(normal)
			return Impl(flat, normal, axes.mapIndexed { index, axis ->
				FlexiDirection.NormalizedImpl(flat[index], normal, by.mirror(axis.tangent))
			})
		}
		
		override fun rotate(by: Rotation): Impl {
			val flat = flatAxes.map { it.rotate(by) }
			val normal = by.rotate(normal)
			return Impl(flat, normal, axes.mapIndexed { index, axis ->
				FlexiDirection.NormalizedImpl(flat[index], normal, by.rotate(axis.tangent))
			})
		}
		
		override fun rotateKnown(by: Int): Impl {
			val angle = by.toFloat() / FlexiDirection.Known.DivisionCount * PI.toFloat()
			val flat = flatAxes.map { it.rotateKnown(by) }
			val normal = normal.yRot(angle)
			return Impl(flat, normal, axes.mapIndexed { index, axis ->
				FlexiDirection.NormalizedImpl(flat[index], normal, axis.tangent.yRot(angle))
			})
		}
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x10)
			tag.put("FlatAxes", ListTag().also { list ->
				val result = if(flatAxes.all { it is FlexiDirection.Known }) {
					flatAxes.map { (it as FlexiDirection.Known).writeInt() }
				} else {
					flatAxes.map { it.write() }
				}
				list.addAll(result)
			})
			tag.put("Normal", VecHelper.writeNBT(normal))
		}
	}
	
	companion object {
		fun read(tag: CompoundTag): FlexiShape = when(tag.getByte("Type")) {
			0x0.toByte() -> Single.read(tag)
			0x10.toByte() -> Impl.read(tag)
			else -> TODO()
		}
	}
}
