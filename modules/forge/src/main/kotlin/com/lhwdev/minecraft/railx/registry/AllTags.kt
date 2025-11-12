package com.lhwdev.minecraft.railx.registry

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block


object AllTags {
	val Registry = RailXRegistry
	
	
	object Features {
		val FlexiTrack = feature("flexi_track")
		val SplitGraph = feature("split_graph")
		
		private fun feature(name: String) = commonTag("feature.$name")
	}
	
	class CommonTag(location: ResourceLocation) {
		val block: TagKey<Block> = TagKey.create(Registries.BLOCK, location)
	}
	
	private fun commonTag(name: String) = CommonTag(Registry.location(name))
}

@Suppress("DEPRECATION")
fun TagKey<Block>.matches(block: Block): Boolean =
	block.builtInRegistryHolder().`is`(this)

fun TagKey<Block>.matches(stack: ItemStack): Boolean =
	(stack.item as? BlockItem)?.let { matches(it.block) } ?: false

