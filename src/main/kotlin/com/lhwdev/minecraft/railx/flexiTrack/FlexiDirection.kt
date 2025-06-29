package com.lhwdev.minecraft.railx.flexiTrack

import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

interface FlexiDirection {
	class Tangent2(val x: Int, val y: Int)

	val tangent: Vec3
	val tangent2: Tangent2?
	val normal: Vec3

	fun mirror(by: Mirror): FlexiDirection
	fun rotate(by: Rotation): FlexiDirection

	class Known(val index: Int) : FlexiDirection, Comparable<Known> {
		companion object {
			const val DivisionCount = 32
			val Divisions = (0 until DivisionCount).map { index -> Known(index) }

			fun roundFromAngle(radian: Double): Known {
				val index = DivisionCount * (radian % PI) / PI
				return Divisions[index.roundToInt() % DivisionCount]
			}
		}

		val angle = PI * (index.toDouble() / DivisionCount)
		val angleDegree = 180 * (index.toDouble() / DivisionCount)

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

		override fun mirror(by: Mirror): Known = when(by) {
			Mirror.NONE -> this
			Mirror.LEFT_RIGHT -> Divisions[(DivisionCount - index) % DivisionCount]
			Mirror.FRONT_BACK -> Divisions[(2 * DivisionCount - index) % DivisionCount]
		}

		override fun rotate(by: Rotation): FlexiDirection =
			Divisions[(2 * DivisionCount + index - by.ordinal * (DivisionCount / 2)) % DivisionCount]

		override fun compareTo(other: Known): Int =
			index - other.index
	}
}