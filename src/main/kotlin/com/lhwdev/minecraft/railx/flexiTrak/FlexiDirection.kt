package com.lhwdev.minecraft.railx.flexiTrak


import com.lhwdev.math.IVec2
import kotlin.math.PI
import kotlin.math.floor


// from 0deg (0rad) to 90deg (pi/2rad)
private val AngleMapping = arrayOf(
	IVec2(41, 1),
	IVec2(61, 3),
	IVec2(27, 2),
	IVec2(61, 6),
	IVec2(57, 7),
	IVec2(27, 4),
	IVec2(23, 4),
	IVec2(5, 1),
	IVec2(49, 11),
	IVec2(4, 1),
	IVec2(47, 13),
	IVec2(56, 17),
	IVec2(61, 20),
	IVec2(14, 5),
	IVec2(57, 22),
	IVec2(29, 12),
	IVec2(52, 23),
	IVec2(55, 26),
	IVec2(2, 1),
	IVec2(43, 23),
	IVec2(30, 17),
	IVec2(5, 3),
	IVec2(30, 19),
	IVec2(3, 2),
	IVec2(44, 31),
	IVec2(31, 23),
	IVec2(41, 32),
	IVec2(39, 32),
	IVec2(29, 25),
	IVec2(32, 29),
	IVec2(21, 20),
	IVec2(1, 1),
	IVec2(20, 21),
	IVec2(29, 32),
	IVec2(25, 29),
	IVec2(32, 39),
	IVec2(32, 41),
	IVec2(23, 31),
	IVec2(31, 44),
	IVec2(2, 3),
	IVec2(19, 30),
	IVec2(3, 5),
	IVec2(17, 30),
	IVec2(23, 43),
	IVec2(1, 2),
	IVec2(26, 55),
	IVec2(23, 52),
	IVec2(17, 41),
	IVec2(22, 57),
	IVec2(5, 14),
	IVec2(20, 61),
	IVec2(17, 56),
	IVec2(13, 47),
	IVec2(1, 4),
	IVec2(11, 49),
	IVec2(1, 5),
	IVec2(4, 23),
	IVec2(4, 27),
	IVec2(7, 57),
	IVec2(6, 61),
	IVec2(2, 27),
	IVec2(3, 61),
	IVec2(1, 41),
)

private const val AngleMappingSize = 16

private val AngleMappingMultiplier = AngleMapping.size / AngleMappingSize


private const val HalfPi = PI / 2

private val Step = HalfPi / AngleMappingSize

fun getFlexiDirection(angle: Double): IVec2 =
	getFlexiDirection(ordinal = getFlexiOrdinal(angle))

private fun getFlexiOrdinal(angle: Double): Int {
	val coercedAngle = floor((angle + Step / 2).mod(HalfPi))
	return (coercedAngle * AngleMappingSize / PI).toInt().coerceAtMost(AngleMappingSize * 2)
}

fun getFlexiDirection(ordinal: Int): IVec2 {
	val result = AngleMapping[AngleMappingMultiplier * (ordinal % AngleMappingSize)]
	return if(ordinal < AngleMappingSize) {
		result
	} else {
		IVec2(-result.y, result.x)
	}
}


class FlexiDirection private constructor(val ordinal: Int, val direction: IVec2) {
	companion object {
		val HalfMaxOrdinal = AngleMappingSize
		
		val MaxOrdinal = HalfMaxOrdinal * 2
		
		private val cache = Array(MaxOrdinal) { ordinal ->
			FlexiDirection(ordinal, getFlexiDirection(ordinal))
		}
		
		fun fromOrdinal(ordinal: Int): FlexiDirection =
			cache[ordinal]
		
		fun fromAngle(angle: Double): FlexiDirection =
			cache[getFlexiOrdinal(angle)]
	}
}
