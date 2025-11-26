package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.utils.averageOf
import it.unimi.dsi.fastutil.objects.Reference2ShortOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import xfacthd.framedblocks.api.block.FramedBlockEntity
import xfacthd.framedblocks.api.block.FramedProperties
import xfacthd.framedblocks.api.block.IFramedBlock
import xfacthd.framedblocks.api.camo.CamoContainer
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity


object FramedBlockMaterialResolver : BlockMaterialResolver {
	override val priority: Int
		get() = 200
	
	
	private val cache = Reference2ShortOpenHashMap<BlockState>()
	
	override fun resolve(
		level: LevelReader,
		pos: BlockPos,
		state: BlockState,
	): BlockMaterial? {
		if(state != level.getBlockState(pos)) return null
		if(state.block !is IFramedBlock) return null
		
		val be = level.getBlockEntity(pos) as? FramedBlockEntity ?: return null
		
		val key = state
			.trySetValue(FramedProperties.GLOWING, false)
			.trySetValue(FramedProperties.PROPAGATES_SKYLIGHT, false)
			.trySetValue(FramedProperties.STATE_LOCKED, false)
			.trySetValue(FramedProperties.FACING_HOR, Direction.NORTH)
			.trySetValue(FramedProperties.FACING_NE, Direction.NORTH)
		var volume = cache.getShort(key).toInt()
		if(volume == 0) {
			volume = key.getCollisionShape(level, pos).calculateVolume()
			cache.put(key, volume.toShort())
		}
		
		// TODO: implement precise calculation for double blocks
		val camos = if(be is FramedDoubleBlockEntity) {
			listOf(be.camo, be.camoTwo)
		} else {
			listOf(be.camo)
		}.map { BlockMaterials.resolve(level, pos, it.state) }
		
		return BlockMaterial(
			priority = camos.averageOf { it.priority } - 100,
			mass = (volume.toDouble() / (16 * 16 * 16)) * camos.averageOf { it.mass },
			debugSource = "${state.blockHolder.unwrapKey().get().location()}($camos)",
		)
	}
	
	override fun cacheKey(level: LevelReader, pos: BlockPos, state: BlockState): CacheKey? {
		if(state.block !is IFramedBlock) return null
		val camo = (level.getBlockEntity(pos) as? FramedBlockEntity)?.camo ?: return null
		return CacheKey(state, camo)
	}
	
	data class CacheKey(val state: BlockState, val camo: CamoContainer)
}

