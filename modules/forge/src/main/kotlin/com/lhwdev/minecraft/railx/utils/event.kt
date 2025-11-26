package com.lhwdev.minecraft.railx.utils

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import java.util.function.Consumer


inline fun <reified T : Event> addOneTimeListener(crossinline callback: (T) -> Unit) {
	val bus = MinecraftForge.EVENT_BUS
	val listener = object : Consumer<T> {
		override fun accept(t: T) {
			try {
				callback(t)
			} finally {
				bus.unregister(this)
			}
		}
	}
	bus.addListener(EventPriority.NORMAL, false, T::class.java, listener)
}
