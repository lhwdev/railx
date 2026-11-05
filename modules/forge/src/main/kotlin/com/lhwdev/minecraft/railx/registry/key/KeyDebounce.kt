package com.lhwdev.minecraft.railx.registry.key

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant


interface KeyDebounce {
	val isHolding: Boolean
	
	val isPressed: Boolean
	
	
	fun onInput(down: Boolean, now: Instant)
	
	/**
	 * Invoked on every frame, only if [isHolding] is true.
	 */
	fun update(now: Instant)
	
	/**
	 * Returns the current [isPressed] value and clears a pending activation, if any.
	 * Continuous presses such as [Repeating] remain active until released or dismissed.
	 * Value of [isPressed] might change after return.
	 */
	fun consumePressed(): Boolean
	
	fun dismissHold()
	
	fun reset()
	
	
	open class Hold : KeyDebounce {
		final override var isHolding = false
			private set
		
		final override var isPressed: Boolean = false
			protected set
		
		override fun onInput(down: Boolean, now: Instant) {
			if(down == isHolding) return
			
			isHolding = down
			if(down) isPressed = true
		}
		
		override fun update(now: Instant) {}
		
		override fun consumePressed(): Boolean = isPressed
			.also { isPressed = false }
		
		override fun dismissHold() {
			isPressed = false
		}
		
		override fun reset() {
			isHolding = false
			isPressed = false
		}
	}
	
	
	class Repeating : KeyDebounce {
		override var isHolding = false
			private set
		
		override var isPressed = false
			private set
		
		override fun onInput(down: Boolean, now: Instant) {
			if(down == isHolding) return
			isHolding = down
			isPressed = down
		}
		
		override fun update(now: Instant) {}
		
		override fun consumePressed(): Boolean =
			isPressed
		
		override fun dismissHold() {
			isPressed = false
		}
		
		override fun reset() {
			isHolding = false
			isPressed = false
		}
	}
	
	
	/** Activates [backing] after [downDelay], releases immediate. */
	class Delay(val backing: KeyDebounce, val downDelay: Duration) : KeyDebounce {
		init {
			require(downDelay >= Duration.ZERO)
		}
		
		override var isHolding = false
			private set
		
		override val isPressed: Boolean
			get() = backing.isPressed
		
		private var downTime: Instant? = null
		
		override fun onInput(down: Boolean, now: Instant) {
			if(down == isHolding) return
			
			isHolding = down
			downTime = if(down) now else null
			
			if(down) update(now)
			else if(backing.isHolding) backing.onInput(false, now)
		}
		
		override fun update(now: Instant) {
			if(!isHolding) return
			val down = downTime
			if(down != null && now - down >= downDelay) {
				downTime = null
				backing.onInput(true, now)
			}
			if(backing.isHolding) backing.update(now)
		}
		
		override fun consumePressed(): Boolean = backing.consumePressed()
		
		override fun dismissHold() {
			downTime = null
			backing.dismissHold()
		}
		
		override fun reset() {
			isHolding = false
			downTime = null
			backing.reset()
		}
	}
	
	
	open class HoldRepeat : Hold() {
		companion object {
			val Delay: Duration = 280.milliseconds
			val Interval: Duration = 80.milliseconds
		}
		
		
		protected class Repeat(val time: Instant, var nextPressTime: Instant)
		
		
		protected var repeat: Repeat? = null
		
		
		override fun onInput(down: Boolean, now: Instant) {
			if(down == isHolding) return
			super.onInput(down, now)
			
			repeat = if(down) {
				Repeat(time = now, nextPressTime = now + calculateDelay(Duration.ZERO))
			} else null
		}
		
		protected open fun calculateDelay(held: Duration): Duration {
			val fraction = ((held - Delay) / (Interval * 5)).coerceIn(0.0, 1.0)
			return Delay + (Interval - Delay) * fraction
		}
		
		override fun update(now: Instant) {
			val down = repeat ?: return
			if(!isHolding || isPressed) return
			
			if(now < down.nextPressTime) return
			
			isPressed = true
			down.nextPressTime = now + calculateDelay(held = now - down.time)
		}
		
		override fun dismissHold() {
			super.dismissHold()
			repeat = null
		}
		
		override fun reset() {
			super.reset()
			repeat = null
		}
	}
	
	
	/** Double click to activate repeat without initial delay. */
	class HoldRepeatWithBoost : HoldRepeat() {
		companion object {
			val BoostWindow = 130.milliseconds
		}
		
		private var lastUpTime: Instant? = null
		
		private var isBoosted = false
		
		override fun onInput(down: Boolean, now: Instant) {
			if(down == isHolding) return
			
			if(down) {
				val lastUp = lastUpTime
				isBoosted = lastUp != null && (now - lastUp) in Duration.ZERO..BoostWindow
				lastUpTime = null
			} else {
				isBoosted = false
				lastUpTime = now
			}
			super.onInput(down, now)
		}
		
		override fun calculateDelay(held: Duration) =
			if(isBoosted) Interval else super.calculateDelay(held)
		
		override fun reset() {
			super.reset()
			lastUpTime = null
			isBoosted = false
		}
	}
	
	
	open class Group(vararg val keys: KeyDebounce) {
		val isHolding: Boolean get() = keys.any { it.isHolding }
		
		val isPressed: Boolean get() = keys.any { it.isPressed }
		
		
		open fun onInput(changes: Map<KeyDebounce, Boolean>, now: Instant) {
			for(key in keys) {
				changes[key]?.let { key.onInput(it, now) }
			}
		}
		
		fun update(now: Instant) {
			for(key in keys) if(key.isHolding) key.update(now)
		}
		
		fun consumeAll() {
			keys.forEach { it.consumePressed() }
		}
		
		open fun reset() {
			keys.forEach { it.reset() }
		}
	}
	
	
	/** General behavior for text input: repeated press will happen only on last pressed key. */
	class DebounceGroup(vararg keys: KeyDebounce) : Group(*keys) {
		private var latest: KeyDebounce? = null
		
		override fun onInput(changes: Map<KeyDebounce, Boolean>, now: Instant) {
			for(key in keys) if(changes[key] == false && key.isHolding) {
				key.onInput(false, now)
				if(latest == key) latest = null
			}
			
			for(key in keys) if(changes[key] == true && !key.isHolding) {
				key.onInput(true, now)
				if(latest == null) latest = key
				else key.dismissHold()
			}
		}
		
		override fun reset() {
			super.reset()
			latest = null
		}
	}
}
