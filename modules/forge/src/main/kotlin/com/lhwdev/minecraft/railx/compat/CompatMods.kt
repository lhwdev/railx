package com.lhwdev.minecraft.railx.compat

import net.neoforged.fml.loading.LoadingModList


// Note: should not reference any code outside loading
object CompatMods {
	@JvmField
	val loaded: Set<String> = LoadingModList.get().mods.mapTo(mutableSetOf()) { it.modId }
	
	val railways: Boolean
		get() = "railways" in loaded
}
