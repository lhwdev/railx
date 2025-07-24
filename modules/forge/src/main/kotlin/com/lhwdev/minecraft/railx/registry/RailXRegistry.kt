package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.simibubi.create.foundation.data.CreateRegistrate
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation


val RailXRegistry = RailXRegistrate(RailX.Id)


class RailXRegistrate(modId: String) : CreateRegistrate(modId) {
	fun location(name: String): ResourceLocation =
		ResourceLocation.fromNamespaceAndPath(modid, name)
	
	
	val allBlocks: List<BlockEntry<*>>
		@Suppress("UNCHECKED_CAST")
		get() = getAll(Registries.BLOCK) as List<BlockEntry<*>>
}
