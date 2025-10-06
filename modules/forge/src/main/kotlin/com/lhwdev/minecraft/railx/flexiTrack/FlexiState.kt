package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.rotate.toRotation
import com.lhwdev.minecraft.railx.utils.maybeCompound
import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaternionf
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVector3d


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
	
	
	val shapeCache: List<AxisCache> by lazy(LazyThreadSafetyMode.NONE) { shape.axes.map { AxisCache(it) } }
	
	class AxisCache(direction: FlexiDirection) {
		val rotation = direction.toRotation()
		val rotationValue = Quaternionf(rotation.rotationValue())
	}
	
	
	fun copy(
		baseShape: FlexiShape = this.baseShape,
		tilt: FlexiShapeTilt? = this.tilt,
	): FlexiState = FlexiState(
		baseShape = baseShape,
		tilt = tilt,
	)
	
	fun write(): CompoundTag = CompoundTag().also { tag ->
		tag.put("Shape", shape.write())
		if(tilt != null) tag.put("Tilt", tilt.write())
	}
	
	companion object {
		val Base = FlexiState()
		
		fun read(tag: CompoundTag): FlexiState = FlexiState(
			baseShape = FlexiShape.read(tag.getCompound("Shape")),
			tilt = tag.maybeCompound("Tilt") { FlexiShapeTilt.read(it) },
		)
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
	
	fun write(): CompoundTag = CompoundTag().also { tag ->
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
