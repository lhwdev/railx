package com.lhwdev.minecraft.railx.api


@JvmField
internal val isApiLoaded: Boolean = true

internal inline fun <R> apiOrNull(block: () -> R): R? =
	if(isApiLoaded) block() else null
