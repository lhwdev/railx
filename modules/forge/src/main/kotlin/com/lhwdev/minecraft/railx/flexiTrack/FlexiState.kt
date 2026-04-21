package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiState.AxisCache
import com.lhwdev.minecraft.railx.flexiTrack.rotate.rotationValue
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.railx.utils.maybeCompound
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.toVec3
import com.lhwdev.minecraft.utils.vectors.toVector3d
import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Quaternionf
import org.joml.Quaternionfc


class FlexiState(
	val baseShape: FlexiShape = FlexiShape.Empty,
	val tilt: FlexiShapeTilt? = null,
	// val offset: Vec3 = Vec3.ZERO,
) {
	val shape: FlexiShape
	
	init {
		var shape = baseShape
		if(tilt != null) shape = tilt.applyTo(shape)
		this.shape = shape
	}
	
	
	fun isEmpty(): Boolean =
		baseShape == FlexiShape.Empty && tilt == null
	
	val shapeCache: List<AxisCache> by lazy(LazyThreadSafetyMode.NONE) {
		shape.axes.map { AxisCache(it) }
	}
	
	class AxisCache(direction: FlexiDirection) {
		val rotationValueDouble: Quaterniondc = direction.rotationValue()
		val rotationValue: Quaternionfc = Quaternionf(rotationValueDouble)
	}
	
	
	fun copy(
		baseShape: FlexiShape = this.baseShape,
		tilt: FlexiShapeTilt? = this.tilt,
	): FlexiState = FlexiState(
		baseShape = baseShape,
		tilt = tilt,
	)
	
	fun write(): CompoundTag = CompoundTag { tag ->
		tag.putByte("V", 1)
		tag.put("Shape", baseShape.write())
		if(tilt != null && tilt.rotation similarTo 0.0) tag.put("Tilt", tilt.write())
	}
	
	companion object {
		val Base = FlexiState()
		
		fun read(tag: CompoundTag): FlexiState {
			val version = tag.getByte("V").toInt()
			return when(version) {
				0 -> FlexiState(
					baseShape = FlexiShape.read(tag.getCompound("Shape"))
						.map {
							fun mapKnown(from: FlexiDirection.Known) =
								FlexiDirection.Known.fromOrdinal(FlexiDirection.Known.DivisionCount - from.ordinal)
							
							when(it) {
								is FlexiDirection.Known -> mapKnown(it)
								is FlexiDirection.Normalized -> {
									val base = it.base
									FlexiDirection.NormalizedImpl(
										base = if(base is FlexiDirection.Known) mapKnown(base) else base,
										normal = it.normal
									)
								}
								
								else -> it
							}
						},
					tilt = tag.maybeCompound("Tilt") { FlexiShapeTilt.read(it) },
				)
				
				1 -> FlexiState(
					baseShape = FlexiShape.read(tag.getCompound("Shape")),
					tilt = tag.maybeCompound("Tilt") { FlexiShapeTilt.read(it) },
				)
				
				else -> error("unknown version")
			}
		}
	}
}


data class FlexiShapeTilt(val axis: Vec3, val rotation: Double) {
	private val rotationValue = Quaterniond()
		.rotationAxis(rotation, axis.x, axis.y, axis.z)
	
	fun applyTo(shape: FlexiShape): FlexiShape = when(shape) {
		FlexiShape.Empty -> FlexiShape.Empty
		is FlexiShape.Single -> FlexiShape.Single(applyTo(shape.axis))
		is FlexiShape.Impl -> FlexiShape.Impl(shape.axes.map { applyTo(it) })
		is FlexiShape.NormalizedImpl -> FlexiShape.NormalizedImpl(
			flatAxes = shape.flatAxes,
			normal = applyTo(shape.normal),
			axes = shape.axes.map { applyTo(it) },
		)
	}
	
	fun applyTo(direction: FlexiDirection): FlexiDirection = if(direction is FlexiDirection.Normalized) {
		applyTo(direction)
	} else {
		FlexiDirection.Two(tangent = applyTo(direction.tangent), normal = applyTo(direction.normal))
	}
	
	fun applyTo(direction: FlexiDirection.Normalized): FlexiDirection.Normalized = FlexiDirection.NormalizedImpl(
		base = direction.base,
		normal = applyTo(FlexiDirection.Flat.normal),
		tangent = applyTo(direction.tangent),
	)
	
	fun applyTo(vec: Vec3): Vec3 = vec.toVector3d().rotate(rotationValue).toVec3()
	
	fun write(): CompoundTag = CompoundTag { tag ->
		tag.put("Axis", VecHelper.writeNBT(axis))
		tag.putDouble("Rotation", rotation)
	}
	
	
	companion object {
		fun read(tag: CompoundTag): FlexiShapeTilt = FlexiShapeTilt(
			axis = VecHelper.readNBT(tag.get("Axis") as ListTag),
			rotation = tag.getDouble("Rotation"),
		)
	}
}
