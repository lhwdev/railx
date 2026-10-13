package com.lhwdev.minecraft.railx.throttle

import kotlin.math.roundToInt


/**
 * Simulates repeated key presses while a key stays held, so gear handling can keep treating one press as
 * one notch.
 *
 * The press itself counts: holding for no longer than [delayTicks] is exactly one press, after which the
 * key counts as pressed once every [intervalTicks].
 */
class HoldRepeat(
	private val delayTicks: () -> Int,
	private val intervalTicks: () -> Int,
) {
	private var heldTicks = 0
	private var nextPressTick = 0
	private var held = false
	private var tick = 0
	
	fun reset() {
		held = false
		heldTicks = 0
	}
	
	fun isPressedDown(actuallyDown: Boolean): Boolean {
		tick++
		if(!actuallyDown) {
			reset()
			return false
		}
		
		heldTicks++
		if(!held) {
			held = true
			nextPressTick = tick
		}
		
		if(tick < nextPressTick) return false
		
		nextPressTick = tick + if(heldTicks == 1) delayTicks() else intervalTicks()
		return true
	}
	
	companion object {
		fun millisToTicks(millis: Int): Int = (millis / 50.0).roundToInt().coerceAtLeast(1)
	}
}
