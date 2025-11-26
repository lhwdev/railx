package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.realisticSpeed.BlockMaterialResolver
import com.lhwdev.minecraft.railx.realisticSpeed.BlockMaterialResolvers
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraftforge.registries.RegistryBuilder


object AllCustoms {
	val Registry = RailXRegistry
	
	val BlockMaterials: ResourceKey<Registry<BlockMaterialResolver>> =
		Registry.makeRegistry("railx.realistic_speed/materials") { RegistryBuilder() }
	
	
	fun register() {
		BlockMaterialResolvers.register()
	}
}
