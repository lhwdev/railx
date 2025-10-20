package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.simibubi.create.foundation.data.CreateRegistrate
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab


val RailXRegistry = RailXRegistrate(RailX.Id)


class RailXRegistrate(modId: String) : CreateRegistrate(modId) {
	init {
		@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		defaultCreativeTab(null as ResourceKey<CreativeModeTab>?)
	}
	
	fun location(name: String): ResourceLocation =
		ResourceLocation.fromNamespaceAndPath(modid, name)
	
	@Suppress("UNCHECKED_CAST")
	fun <T> registryOf(key: ResourceKey<Registry<T>>): Registry<T> =
		BuiltInRegistries.REGISTRY[key.location()] as Registry<T>
	
	
	val allBlocks: List<BlockEntry<*>>
		@Suppress("UNCHECKED_CAST")
		get() = getAll(Registries.BLOCK) as List<BlockEntry<*>>
	
	
	fun <T> dataComponentType(
		name: String,
		block: DataComponentType.Builder<T>.() -> Unit,
	): RegistryEntry<DataComponentType<*>, DataComponentType<T>> =
		generic(name, Registries.DATA_COMPONENT_TYPE) { DataComponentType.builder<T>().apply(block).build() }
			.register()
}
