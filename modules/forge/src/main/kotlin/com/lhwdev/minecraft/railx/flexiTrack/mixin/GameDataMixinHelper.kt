package com.lhwdev.minecraft.railx.flexiTrack.mixin

import com.lhwdev.minecraft.railx.flexiTrack.FlexiBlockState
import net.minecraft.core.IdMapper
import net.minecraft.world.level.block.state.BlockState
import java.util.*


object GameDataMixinHelper {
	private val cache = IdentityHashMap<IdMapper<BlockState>, FlexiIdMapper>()
	
	fun mapBlockStateIDMap(map: IdMapper<BlockState>): IdMapper<BlockState> =
		cache.getOrPut(map) { FlexiIdMapper(map) }
}

private class FlexiIdMapper(private val base: IdMapper<BlockState>) : IdMapper<BlockState>(0) {
	init {
		tToId = base.tToId
		idToT = base.idToT
	}
	
	override fun addMapping(key: BlockState, value: Int) {
		base.addMapping(key, value)
	}
	
	override fun getId(value: BlockState): Int {
		val result = super.getId(value)
		if(result != -1) return result
		if(value is FlexiBlockState) return super.getId(value.base)
		return -1
	}
}
