package com.lhwdev.minecraft.railx.flexiTrack

import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


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

interface FlexiDirection {
	class Tangent2(val x: Int, val y: Int)
	
	val tangent: Vec3
	val tangent2: Tangent2?
	val normal: Vec3
	
	fun mirror(by: Mirror): FlexiDirection
	fun rotate(by: Rotation): FlexiDirection
	
	class FlatDivision(val index: Int) : FlexiDirection, Comparable<FlatDivision> {
		companion object {
			const val DivisionCount = 32
			val Divisions = (0 until DivisionCount).map { index -> FlatDivision(index) }
		}
		
		val angle = PI * (index.toDouble() / DivisionCount)
		
		override val tangent: Vec3 = Vec3(cos(angle), 0.0, sin(angle))
		
		override val tangent2: Tangent2?
			get() = if(index % (DivisionCount / 4) == 0) {
				when(index / (DivisionCount / 4)) {
					0 -> Tangent2(1, 0)
					1 -> Tangent2(1, 1)
					2 -> Tangent2(0, 1)
					3 -> Tangent2(-1, 0)
					else -> error("unreachable")
				}
			} else {
				null
			}
		
		override val normal: Vec3
			get() = Vec3(0.0, 1.0, 0.0)
		
		override fun mirror(by: Mirror): FlatDivision = when(by) {
			Mirror.NONE -> this
			Mirror.LEFT_RIGHT -> Divisions[(DivisionCount - index) % DivisionCount]
			Mirror.FRONT_BACK -> Divisions[(2 * DivisionCount - index) % DivisionCount]
		}
		
		override fun rotate(by: Rotation): FlexiDirection =
			Divisions[(2 * DivisionCount + index - by.ordinal * (DivisionCount / 2)) % DivisionCount]
		
		override fun compareTo(other: FlatDivision): Int =
			index - other.index
	}
}
