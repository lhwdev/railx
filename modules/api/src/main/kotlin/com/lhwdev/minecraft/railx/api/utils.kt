package com.lhwdev.minecraft.railx.api

import com.lhwdev.minecraft.railx.RailX
import net.neoforged.fml.ModList


// Note: as RailX.Id is const val, resolved as LDC; no reference to RailX class
@JvmField
internal val isApiLoaded: Boolean = ModList.get().isLoaded(RailX.Id)

internal inline fun <R> apiOrNull(block: () -> R): R? =
	if(isApiLoaded) block() else null
