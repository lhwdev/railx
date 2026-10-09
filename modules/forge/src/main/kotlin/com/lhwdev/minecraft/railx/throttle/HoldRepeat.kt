package com.lhwdev.minecraft.railx.throttle

import kotlin.math.roundToInt


/**
 * Simulates repeated key presses while a key stays held.
 *
 * This exists so the throttle can keep its original "one press moves one notch" logic untouched: instead of
 * teaching the gear handling about hold durations, a held key simply counts as pressed again every so often.
 *
 * Timing for one hold:
 *  - the press itself counts, so holding for no longer than [delayTicks] is exactly one press;
 *  - after that the key counts as pressed once every [intervalTicks].
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
	
	/** @return true when the key should count as pressed on this tick. */
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
		
		// the press after this one comes `interval` later, except the first which waits `delay`
		nextPressTick = tick + if(heldTicks == 1) delayTicks() else intervalTicks()
		return true
	}
	
	companion object {
		/** One game tick is 50ms; a duration that is not a multiple of 50 is rounded, never down to zero. */
		fun millisToTicks(millis: Int): Int = (millis / 50.0).roundToInt().coerceAtLeast(1)
	}
}
