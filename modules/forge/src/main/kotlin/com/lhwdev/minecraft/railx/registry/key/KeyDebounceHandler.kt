package com.lhwdev.minecraft.railx.registry.key

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource


fun KeyDebounceHandler.registerKeyEvent() {
	for(key in keys) {
		key.onKey { onInput(it) }
	}
	
	KeyDebounceHandler.frameHandlers += { update() }
}

fun KeyDebounceHandler.registerGlobalKeyEvent() {
	AllKeys.onGlobalKey { onInput(it) }
	KeyDebounceHandler.frameHandlers += { update() }
}


/** Binds [AllKeys] to [KeyDebounce]. */
open class KeyDebounceHandler {
	companion object {
		internal val frameHandlers = mutableSetOf<() -> Unit>()
		
		fun onFrame() {
			frameHandlers.forEach { it() }
		}
	}
	
	
	private val clock = MonotonicClock()
	
	open val now: Instant
		get() = clock.now
	
	
	private val bindings = linkedMapOf<AllKeys, KeyDebounce>()
	
	private val groups = mutableListOf<KeyDebounce.Group>()
	
	
	val keys: Collection<AllKeys>
		get() = bindings.keys
	
	
	protected infix fun <T : KeyDebounce> AllKeys.bind(debounce: T): T {
		require(this !in bindings && debounce !in bindings.values)
		bindings[this] = debounce
		return debounce
	}
	
	protected fun group(group: KeyDebounce.Group) {
		require(group.keys.all { it in bindings.values && groups.none { previous -> it in previous.keys } })
		groups += group
	}
	
	protected fun group(vararg groups: KeyDebounce.Group) {
		groups.forEach { group(it) }
	}
	
	
	open fun onInput(key: AllKeys, now: Instant = this.now) {
		val debounce = bindings[key] ?: return
		val group = groups.find { debounce in it.keys }
		if(group != null) group.onInput(mapOf(debounce to key.isDown), now)
		else debounce.onInput(key.isDown, now)
	}
	
	open fun update(now: Instant = this.now) {
		bindings.values.filter { it.isHolding }.forEach { it.update(now) }
	}
	
	fun consumeAll() {
		bindings.values.forEach { it.consumePressed() }
	}
	
	open fun reset() {
		groups.forEach { it.reset() }
		bindings.values.forEach { it.reset() }
	}
}


private class MonotonicClock {
	
	private val timeOrigin = Clock.System.now()
	
	private val monotonicOrigin = TimeSource.Monotonic.markNow()
	
	val now: Instant
		get() = timeOrigin + monotonicOrigin.elapsedNow()
}
