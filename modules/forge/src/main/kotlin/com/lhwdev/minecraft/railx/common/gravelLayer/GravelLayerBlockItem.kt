package com.lhwdev.minecraft.railx.common.gravelLayer

import com.lhwdev.minecraft.railx.common.gravelLayer.GravelLayerBlock.Companion.Layers
import com.lhwdev.minecraft.railx.utils.averageOf
import com.lhwdev.minecraft.railx.utils.math.BlockItemUtils
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.gameevent.GameEvent


class GravelLayerBlockItem(block: Block, properties: Properties) : BlockItem(block, properties) {
	override fun place(context: BlockPlaceContext): InteractionResult {
		if(!context.canPlace()) return InteractionResult.FAIL
		
		val context = updatePlacementContext(context)
			?: return InteractionResult.FAIL
		
		val level = context.level
		val player = context.player ?: return InteractionResult.FAIL
		val pos = context.clickedPos
		val stack = context.itemInHand
		
		var state = getPlacementState(context)
			?: return InteractionResult.FAIL
		
		var layers = 1
		
		if(player.isShiftKeyDown) {
			val surrounding = Direction.Plane.HORIZONTAL
				.map { level.getBlockState(pos.offset(it.normal)) }
				.filter { it.block == block }
			
			if(surrounding.isNotEmpty()) {
				layers = surrounding
					.averageOf { it.getValue(Layers) }
					.coerceIn(1, 8)
			}
			
			if(stack.count >= layers || player.hasInfiniteMaterials()) {
				val previous = state.getValue(Layers)
				if(layers > previous) state = state.setValue(Layers, layers)
			} else {
				layers = 1
			}
		}
		
		if(!placeBlock(context, state))
			return InteractionResult.FAIL
		
		var stateAtPos = level.getBlockState(pos)
		if(stateAtPos.`is`(state.block)) {
			stateAtPos = BlockItemUtils.updateBlockStateFromTag(pos, level, stack, stateAtPos)
			updateCustomBlockEntityTag(pos, level, player, stack, stateAtPos)
			BlockItemUtils.updateBlockEntityComponents(level, pos, stack)
			stateAtPos.block.setPlacedBy(level, pos, stateAtPos, player, stack)
			
			if(player is ServerPlayer) {
				CriteriaTriggers.PLACED_BLOCK.trigger(player, pos, stack)
			}
		}
		
		val soundType = stateAtPos.getSoundType(level, pos, player)
		level.playSound(
			player,
			pos,
			getPlaceSound(stateAtPos, level, pos, player),
			SoundSource.BLOCKS,
			(soundType.volume + 1.0f) / 2.0f,
			soundType.pitch * 0.8f
		)
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, stateAtPos))
		stack.consume(layers, player)
		return InteractionResult.sidedSuccess(level.isClientSide)
	}
	
}
