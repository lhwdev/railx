package com.lhwdev.minecraft.railx.compat

import com.lhwdev.minecraft.railx.compat.railways.RailwaysCompat


object RailXCompat {
	fun register() {
		if(CompatMods.railways) RailwaysCompat.register()
	}
}
