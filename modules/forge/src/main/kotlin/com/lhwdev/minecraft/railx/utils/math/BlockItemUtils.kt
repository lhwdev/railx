package com.lhwdev.minecraft.railx.utils.math

import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.BlockItemStateProperties
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState


object BlockItemUtils {
	fun updateBlockStateFromTag(pos: BlockPos, level: Level, stack: ItemStack, state: BlockState): BlockState {
		val properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
		
		if(properties.isEmpty) {
			return state
		} else {
			val newState = properties.apply(state)
			if(newState !== state) {
				level.setBlock(pos, newState, Block.UPDATE_CLIENTS)
			}
			
			return newState
		}
	}
	
	fun updateBlockEntityComponents(level: Level, pos: BlockPos, stack: ItemStack) {
		val blockEntity = level.getBlockEntity(pos)
		if(blockEntity != null) {
			blockEntity.applyComponentsFromItemStack(stack)
			blockEntity.setChanged()
		}
	}
}
