package com.lhwdev.minecraft.railx.utils

import net.neoforged.bus.api.Event
import net.neoforged.neoforge.common.NeoForge
import java.util.function.Consumer


inline fun <reified T : Event> addOneTimeListener(crossinline callback: (T) -> Unit) {
	val bus = NeoForge.EVENT_BUS
	val listener = object : Consumer<T> {
		override fun accept(t: T) {
			try {
				callback(t)
			} finally {
				bus.unregister(this)
			}
		}
	}
	bus.addListener(T::class.java, listener)
}
