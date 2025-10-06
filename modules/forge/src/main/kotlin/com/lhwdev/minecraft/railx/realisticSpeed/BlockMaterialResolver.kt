package com.lhwdev.minecraft.railx.realisticSpeed

import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState


interface BlockMaterialResolver {
	val priority: Int
	
	/**
	 * Be aware that [state] may not necessarily be identical to `level.getBlockState(pos)`. This may be used, for
	 * instance, by framed blocks and copycats to inspect camo / inner blocks.
	 */
	fun resolve(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial?
	
	
	fun cacheKey(level: LevelReader, pos: BlockPos, state: BlockState): Any? = null
}
