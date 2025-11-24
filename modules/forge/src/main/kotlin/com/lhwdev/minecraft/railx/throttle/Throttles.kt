package com.lhwdev.minecraft.railx.throttle


object Throttles {
	enum class Reverser(val step: Int) {
		Forward(step = 1),
		Neutral(step = 0),
		Backward(step = -1);
	}
	
	enum class Steering { Left, Neutral, Right }
	
	data class Throttle(
		val reverser: Reverser,
		val steering: Steering,
		val gear: Int,
		val breaking: Boolean,
	) {
		companion object {
			val Neutral = Throttle(reverser = Reverser.Neutral, steering = Steering.Neutral, gear = 0, breaking = false)
		}
	}
}
