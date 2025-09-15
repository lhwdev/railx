package com.lhwdev.minecraft.railx.utils

import net.neoforged.neoforge.common.ModConfigSpec


fun <T> ModConfigSpec.ConfigValue<T>.getOrNull(): T? = try {
	get()
} catch(e: IllegalStateException) {
	null
}
