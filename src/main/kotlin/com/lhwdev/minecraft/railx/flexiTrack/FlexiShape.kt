package com.lhwdev.minecraft.railx.flexiTrack

import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3


interface FlexiShape {
	val axes: List<FlexiDirection>
	val axe1: FlexiDirection get() = axes[0]
	val axe2: FlexiDirection? get() = axes.getOrNull(1)
	
	val tangents: List<Vec3> get() = axes.map { it.tangent }
	val normal: Vec3
	
	fun mirror(by: Mirror): FlexiShape
	fun rotate(by: Rotation): FlexiShape
	
	class Single(val axe: FlexiDirection) : FlexiShape {
		override val axes: List<FlexiDirection>
			get() = listOf(axe)
		override val axe1: FlexiDirection
			get() = axe
		override val axe2: FlexiDirection?
			get() = null
		
		override val tangents: List<Vec3>
			get() = listOf(axe.tangent)
		
		val tangent: Vec3
			get() = axe.tangent
		
		override val normal: Vec3
			get() = axe.normal
	
		override fun mirror(by: Mirror): Single =
			Single(axe.mirror(by))
		
		override fun rotate(by: Rotation): Single =
			Single(axe.rotate(by))
	}
}
