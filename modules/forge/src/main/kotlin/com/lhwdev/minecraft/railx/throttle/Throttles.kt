package com.lhwdev.minecraft.railx.throttle


object Throttles {
	val maxThrottle: Int
		get() = 4
	
	val maxBreak: Int
		get() = 7
	
	private val throttleAccelerations: DoubleArray =
		doubleArrayOf(0.1, 0.4, 0.7, 1.0)
	
	private val breakAccelerations: DoubleArray =
		doubleArrayOf(0.1, 0.2, 0.35, 0.5, 0.67, 0.84, 1.0)
	
	
	enum class Reverser(val step: Int) {
		Forward(step = 1),
		Neutral(step = 0),
		Backward(step = -1);
		
		fun forward(): Reverser = when(this) {
			Forward -> Forward
			Neutral -> Forward
			Backward -> Neutral
		}
		
		fun backward(): Reverser = when(this) {
			Forward -> Neutral
			Neutral -> Backward
			Backward -> Backward
		}
	}
	
	enum class Steering { Left, Neutral, Right }
	
	data class Throttle(val reverser: Reverser, val steering: Steering, val gear: Int) {
		val acceleration: Double
			get() = when {
				gear == 0 -> 0.0
				gear > 0 -> throttleAccelerations[gear - 1]
				else -> -breakAccelerations[-gear - 1]
			}
		
		
		companion object {
			val Neutral = Throttle(reverser = Reverser.Neutral, steering = Steering.Neutral, gear = 0)
		}
	}
}
