@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.minecraft.railx.utils


object ColorsArgb {
	inline fun alpha(argb: Int): Int =
		(argb ushr 24) and 0xff
	
	inline fun red(argb: Int): Int =
		(argb ushr 16) and 0xff
	
	inline fun green(argb: Int): Int =
		(argb ushr 8) and 0xff
	
	inline fun blue(argb: Int): Int =
		(argb ushr 0) and 0xff
	
	inline fun toArgb(alpha: Int, red: Int, green: Int, blue: Int): Int =
		((alpha and 0xff) shl 24) or
			((red and 0xff) shl 16) or
			((green and 0xff) shl 8) or
			((blue and 0xff) shl 0)
	
	inline fun zip(a: Int, b: Int, zipComponent: (a: Int, b: Int) -> Int): Int = toArgb(
		alpha = zipComponent(alpha(a), alpha(b)),
		red = zipComponent(red(a), red(b)),
		green = zipComponent(green(a), green(b)),
		blue = zipComponent(blue(a), blue(b)),
	)
	
	fun multiply(a: Int, b: Int): Int =
		zip(a, b) { i, j -> (i * j) / 255 }
	
	fun lerp(a: Int, b: Int, fraction: Float): Int =
		zip(a, b) { a, b -> a + ((b - a) * fraction).toInt() }
}
