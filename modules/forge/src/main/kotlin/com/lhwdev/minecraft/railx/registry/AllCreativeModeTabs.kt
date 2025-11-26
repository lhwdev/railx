@file:Suppress("unused")

package com.lhwdev.minecraft.railx.registry

import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import kotlin.streams.asSequence
import com.simibubi.create.AllBlocks as CreateBlocks
import com.simibubi.create.AllCreativeModeTabs as CreateCreativeModeTabs


object AllCreativeModeTabs {
	fun register() {}
	
	val Registry = RailXRegistry
	
	val BaseTab: RegistryEntry<CreativeModeTab> = Registry.simple(
		"base_tag",
		Registries.CREATIVE_MODE_TAB,
	) {
		CreativeModeTab.builder()
			.title(Component.literal("RailX"))
			.withTabsBefore(CreateCreativeModeTabs.BASE_CREATIVE_TAB.key)
			.icon { CreateBlocks.TRACK.asStack() }
			.displayItems(RegistrateDisplayItemsGenerator())
			.build()
	}
	
	private class RegistrateDisplayItemsGenerator : CreativeModeTab.DisplayItemsGenerator {
		override fun accept(
			parameters: CreativeModeTab.ItemDisplayParameters,
			output: CreativeModeTab.Output,
		) {
			val features = listOf(
				null,
				AllTags.Features.FlexiTrack.block,
			)
			RailXRegistry.allBlocks
				.filter { !it.asStack().isEmpty }
				.sortedBy { block ->
					val tag = block.holder.get().tags()
						.asSequence()
						.firstOrNull { it.location.path.startsWith("feature.") }
					features.indexOf(tag)
				}
				.forEach { output.accept(it.asStack()) }
		}
	}
}
