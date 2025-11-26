package com.lhwdev.minecraft.railx.utils

import net.minecraftforge.common.ForgeConfigSpec


fun <T> ForgeConfigSpec.ConfigValue<T>.getOrNull(): T? = try {
	get()
} catch(_: IllegalStateException) {
	null
}

fun <T> ForgeConfigSpec.ConfigValue<T>.getOrDefault(default: T): T = try {
	get()
} catch(_: IllegalStateException) {
	default
}

val ForgeConfigSpec.BooleanValue.orFalse: Boolean
	get() = try {
		get()
	} catch(_: IllegalStateException) {
		false
	}
