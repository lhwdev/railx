package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.registry.AllCustoms
import com.lhwdev.minecraft.railx.registry.RailXRegistry
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState


object BlockMaterials {
	private var MaterialResolvers: List<BlockMaterialResolver>? = null
	
	private fun materialResolvers(): List<BlockMaterialResolver> {
		MaterialResolvers?.let { return it }
		
		if(!RailXRegistry.isRegistered(AllCustoms.BlockMaterials)) {
			throw IllegalStateException("do not access BlockMaterials until registration.")
		}
		
		// won't use any registry other than RailXRegistry
		return RailXRegistry.registryOf(AllCustoms.BlockMaterials).toList()
			.also { MaterialResolvers = it.sortedByDescending { it.priority } }
	}
	
	
	fun resolve(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial {
		val resolvers = materialResolvers()
		for(resolver in resolvers) {
			resolver.resolve(level, pos, state)?.let { return it }
		}
		throw NoSuchElementException("no BlockMaterial corresponding to $state found.")
	}
	
	fun cacheKey(level: LevelReader, pos: BlockPos, state: BlockState): Any {
		val resolvers = materialResolvers()
		for(resolver in resolvers) {
			resolver.cacheKey(level, pos, state)?.let { return it }
		}
		throw NoSuchElementException("no cache key corresponding to $state found.")
	}
}
