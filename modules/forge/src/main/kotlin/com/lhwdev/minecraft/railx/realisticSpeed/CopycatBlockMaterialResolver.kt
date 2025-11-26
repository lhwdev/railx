package com.lhwdev.minecraft.railx.realisticSpeed

import com.copycatsplus.copycats.CCBlockStateProperties
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity
import com.lhwdev.minecraft.railx.utils.averageOf
import it.unimi.dsi.fastutil.objects.Reference2ShortOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.Half


object CopycatBlockMaterialResolver : BlockMaterialResolver {
	override val priority: Int
		get() = 200
	
	
	private val cache = Reference2ShortOpenHashMap<BlockState>()
	
	override fun resolve(
		level: LevelReader,
		pos: BlockPos,
		state: BlockState,
	): BlockMaterial? {
		if(state != level.getBlockState(pos)) return null
		val block = state.block
		if(block !is ICopycatBlock) return null
		val be = block.getCopycatBlockEntity(level, pos) ?: return null
		
		val key = state
			.trySetValue(BlockStateProperties.FACING, Direction.DOWN)
			.trySetValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.trySetValue(BlockStateProperties.HALF, Half.TOP)
			.trySetValue(CCBlockStateProperties.SIDE, CCBlockStateProperties.Side.LEFT)
			.trySetValue(BlockStateProperties.AXIS, Direction.Axis.X)
		var volume = cache.getShort(key).toInt()
		if(volume == 0) {
			volume = key.getCollisionShape(level, pos).calculateVolume()
			cache.put(key, volume.toShort())
		}
		
		// TODO: implement precise calculation for double blocks
		val materials = if(be is IMultiStateCopycatBlockEntity) {
			be.materialItemStorage.allMaterials.map { BlockMaterials.resolve(level, pos, it) }
		} else {
			listOf(BlockMaterials.resolve(level, pos, be.material))
		}
		
		return BlockMaterial(
			priority = materials.averageOf { it.priority } - 100,
			mass = (volume.toDouble() / BlockVolume) * materials.averageOf { it.mass },
			debugSource = "${state.blockHolder.unwrapKey().get().location()}(materials = $materials)",
		)
	}
	
	override fun cacheKey(level: LevelReader, pos: BlockPos, state: BlockState): CacheKey? {
		val block = state.block as? ICopycatBlock ?: return null
		val be = block.getCopycatBlockEntity(level, pos) ?: return null
		return if(be is IMultiStateCopycatBlockEntity) {
			CacheKey(state, be.materialItemStorage.allMaterials.toList())
		} else {
			CacheKey(state, listOf(be.material))
		}
	}
	
	data class CacheKey(val state: BlockState, val materials: List<BlockState>? = null)
}
