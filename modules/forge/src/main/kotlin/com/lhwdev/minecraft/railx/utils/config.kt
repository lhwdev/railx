package com.lhwdev.minecraft.railx.utils

import net.neoforged.neoforge.common.ModConfigSpec


fun <T> ModConfigSpec.ConfigValue<T>.getOrNull(): T? = try {
	get()
} catch(_: IllegalStateException) {
	null
}

fun <T> ModConfigSpec.ConfigValue<T>.getOrDefault(default: T): T = try {
	get()
} catch(_: IllegalStateException) {
	default
}

val ModConfigSpec.BooleanValue.orFalse: Boolean
	get() = try {
		isTrue
	} catch(_: IllegalStateException) {
		false
	}
