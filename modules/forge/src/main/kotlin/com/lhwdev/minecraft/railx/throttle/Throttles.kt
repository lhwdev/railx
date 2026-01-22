package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.utils.CompoundTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag


object Throttles {
	val maxThrottle: Int
		get() = 4
	
	val maxBreak: Int
		get() = 7
	
	private val throttleAccelerations: DoubleArray =
		doubleArrayOf(0.15, 0.4, 0.7, 1.0)
	
	private val breakAccelerations: DoubleArray =
		doubleArrayOf(0.12, 0.24, 0.37, 0.50, 0.66, 0.83, 1.0)
	
	
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
		
		fun reverse(): Reverser = when(this) {
			Forward -> Backward
			Neutral -> Neutral
			Backward -> Forward
		}
	}
	
	enum class Steering { Left, Neutral, Right }
	
	data class Throttle(val reverser: Reverser, val steering: Steering, val gear: Int) {
		val acceleration: Double
			get() = when {
				gear == 0 -> 0.0
				gear > 0 -> throttleAccelerations.getOrElse(gear - 1) { 1.0 }
				else -> -breakAccelerations.getOrElse(-gear - 1) { 1.0 }
			}
		
		fun reverse(): Throttle = Throttle(reverser = reverser.reverse(), steering, gear)
		
		fun reverseIf(forward: Boolean): Throttle = if(forward) this else reverse()
		
		fun write(): Tag {
			if(this == Neutral) return ByteTag.valueOf(0)
			
			return CompoundTag { tag ->
				tag.putString("Reverser", reverser.name)
				tag.putString("Steering", steering.name)
				tag.putInt("Gear", gear)
			}
		}
		
		
		companion object {
			@JvmField
			val Neutral = Throttle(reverser = Reverser.Neutral, steering = Steering.Neutral, gear = 0)
			
			@JvmField
			val NeutralStop = Throttle(reverser = Reverser.Neutral, steering = Steering.Neutral, gear = -7)
			
			fun read(tag: Tag): Throttle = when(tag) {
				is CompoundTag -> Throttle(
					reverser = Reverser.valueOf(tag.getString("Reverser")),
					steering = Steering.valueOf(tag.getString("Steering")),
					gear = tag.getInt("Gear"),
				)
				
				is NumericTag -> when(tag.asInt) {
					0 -> Neutral
					else -> error("unexpected int ${tag.asInt}")
				}
				
				else -> error("unexpected tag type ${tag.type.name}; $tag")
			}
		}
	}
}
