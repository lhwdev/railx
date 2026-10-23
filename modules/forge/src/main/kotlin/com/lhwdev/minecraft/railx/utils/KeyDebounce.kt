package com.lhwdev.minecraft.railx.utils

import com.lhwdev.minecraft.railx.registry.AllKeys
import net.minecraft.util.Mth
import java.util.*

interface KeyDebounce {
	fun reset()
	
	fun tickPressed(deltaTicks: Int = 1)
	
	fun dismissHold() {}
	
	val isHolding: Boolean
	val isPressed: Boolean
	
	
	/** Persists hold unless releasing key. */
	class Hold(val holding: () -> Boolean) : KeyDebounce {
		constructor(key: AllKeys) : this(holding = { key.isPressed })
		
		override var isHolding: Boolean = false
			private set
		override var isPressed: Boolean = false
			private set
		
		override fun reset() {
			isHolding = false
			isPressed = false
		}
		
		override fun tickPressed(deltaTicks: Int) {
			val down = holding()
			if(!isHolding && down) {
				isHolding = true
				isPressed = true
			} else {
				isHolding = down
				isPressed = false
			}
		}
		
		override fun dismissHold() {
			isPressed = false
		}
	}
	
	
	/**
	 * While a key is hold, simulates as repeated click.
	 * This might be the most general model for key press.
	 */
	open class HoldRepeat(val holding: () -> Boolean) : KeyDebounce {
		constructor(key: AllKeys) : this(holding = { key.isPressed })
		
		companion object {
			const val DelayTicks = 6
			const val IntervalTicks = 2
		}
		
		
		override var isPressed: Boolean = false
			protected set
		
		protected var heldTicks = 0
		protected var nextPressTick = 0L
		protected var tick = 0L
		
		override fun reset() {
			heldTicks = 0
		}
		
		override fun tickPressed(deltaTicks: Int) {
			isPressed = false
			tick += deltaTicks
			
			if(!holding()) {
				reset()
				return
			}
			
			val previousHeldTicks = heldTicks
			heldTicks += deltaTicks
			
			if(previousHeldTicks == 0) {
				isPressed = true
				nextPressTick = tick + calculateDelayTicks(heldTicks = 0)
				return
			}
			
			if(tick >= nextPressTick) {
				isPressed = true
				nextPressTick = tick + calculateDelayTicks(heldTicks)
			}
		}
		
		open fun calculateDelayTicks(heldTicks: Int) = if(heldTicks == 0) {
			DelayTicks
		} else {
			val fraction = (heldTicks - DelayTicks - IntervalTicks).toFloat() / IntervalTicks / 6
			Mth.lerpInt(fraction.coerceIn(0f, 1f), DelayTicks, IntervalTicks)
		}
		
		override fun dismissHold() {
			isPressed = false
			nextPressTick = Long.MAX_VALUE
		}
		
		override val isHolding: Boolean
			get() = heldTicks > 0
	}
	
	open class HoldRepeatWithBoost(holding: () -> Boolean) : HoldRepeat(holding) {
		constructor(key: AllKeys) : this(holding = { key.isPressed })
		
		companion object {
			const val BoostDelayMaxTicks = 3
		}
		
		protected var lastUpTick = -100L
		protected var isBoosted = false
		
		override fun reset() {
			super.reset()
			isBoosted = false
		}
		
		override fun tickPressed(deltaTicks: Int) {
			if(heldTicks in 1..3 && !holding()) {
				lastUpTick = tick
			}
			super.tickPressed(deltaTicks)
		}
		
		override fun calculateDelayTicks(heldTicks: Int): Int {
			if(heldTicks == 0 && tick - lastUpTick <= BoostDelayMaxTicks) {
				isBoosted = true
			}
			return if(isBoosted) {
				IntervalTicks
			} else {
				super.calculateDelayTicks(heldTicks)
			}
		}
	}
	
	
	/**
	 * For keys like [HoldRepeat], when one key is hold down and another key is down then first key is processed as if
	 * it is released. This simulates these behavior.
	 *
	 * If multiple is pressed simultaneously, first wins.
	 */
	class DebounceGroup(val keys: List<KeyDebounce>) : KeyDebounce {
		constructor(vararg keys: KeyDebounce) : this(listOf(*keys))
		
		private val state = BitSet(keys.size)
		private var latest: KeyDebounce? = null
		
		override fun reset() {
			latest = null
		}
		
		fun resetAll() {
			reset()
			for(key in keys) key.reset()
		}
		
		override fun tickPressed(deltaTicks: Int) {
			var latest: KeyDebounce? = null
			
			for((index, key) in keys.withIndex()) {
				val previous = state[index]
				
				key.tickPressed(deltaTicks = deltaTicks)
				var current = key.isHolding
				if(previous != current) {
					if(current) {
						if(latest == null) {
							latest = key
						} else {
							key.dismissHold()
							current = key.isHolding
						}
					}
					
					state[index] = current
				}
			}
			
			if(latest == null) {
				reset()
				return
			}
		}
		
		override val isHolding: Boolean
			get() = state.nextSetBit(0) != -1
		
		override val isPressed: Boolean
			get() = keys.any { it.isPressed }
	}
}
