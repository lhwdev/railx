package com.lhwdev.minecraft.railx.flexiTrack

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.BlockGetter


abstract class FlexiState(
	shape: FlexiShape,
) {
	var shape: FlexiShape = shape
		private set
	
	fun updateShape(shape: FlexiShape) {
		this.shape = shape
	}
	
	inline fun updateShape(block: (FlexiShape) -> FlexiShape) {
		updateShape(block(shape))
	}
	
	fun write(): CompoundTag {
		val tag = CompoundTag()
		return tag
	}
}

fun BlockGetter.flexiState(pos: BlockPos): FlexiState =
	(getBlockEntity(pos) as FlexiTrackBlockEntity).state

fun BlockGetter.flexiShape(pos: BlockPos): FlexiShape =
	flexiState(pos).shape
