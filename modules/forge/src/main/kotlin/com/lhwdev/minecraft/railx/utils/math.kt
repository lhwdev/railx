package com.lhwdev.minecraft.railx.utils


infix fun Double.floorMod(by: Double): Double {
	val r = this % by
	
	return if(this * by < 0 && r != 0.0) r + by
	else r
}