package com.lhwdev.minecraft.railx.registry

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block


object AllTags {
	val Registry = RailXRegistry
	
	
	object Features {
		val FlexiTrack = feature("flexi_track")
		
		private fun feature(name: String) = commonTag("feature.$name")
	}
	
	class CommonTag(location: ResourceLocation) {
		val block: TagKey<Block> = TagKey.create(Registries.BLOCK, location)
	}
	
	private fun commonTag(name: String) = CommonTag(Registry.location(name))
}
