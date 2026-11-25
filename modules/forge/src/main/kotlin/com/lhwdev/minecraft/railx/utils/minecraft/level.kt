package com.lhwdev.minecraft.railx.utils.minecraft

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity


fun LevelAccessor.dimension(): ResourceKey<Level> =
	(this as? Level)?.dimension() ?: Level.OVERWORLD

fun LevelReader.getBlockEntityIfLoaded(pos: BlockPos): BlockEntity? =
	@Suppress("DEPRECATION")
	if(hasChunk(SectionPos.blockToSectionCoord(pos.x), SectionPos.blockToSectionCoord(pos.z))) {
		getBlockEntity(pos)
	} else {
		null
	}
