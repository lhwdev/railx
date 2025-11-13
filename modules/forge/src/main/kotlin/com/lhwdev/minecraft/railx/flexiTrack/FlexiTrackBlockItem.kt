package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.content.trains.track.TrackBlockItem
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import com.simibubi.create.AllBlocks as CreateBlocks
import com.simibubi.create.AllDataComponents as CreateDataComponents
import com.simibubi.create.AllSoundEvents as CreateSoundEvents
import com.simibubi.create.AllTags as CreateTags


class FlexiTrackBlockItem(block: Block, properties: Properties) : TrackBlockItem(block, properties) {
	override fun useOn(pContext: UseOnContext): InteractionResult {
		var stack = pContext.itemInHand
		var pos = pContext.clickedPos
		val level = pContext.level
		var state = level.getBlockState(pos)
		val player = pContext.player ?: return super.useOn(pContext)
		
		if(pContext.hand == InteractionHand.OFF_HAND) return super.useOn(pContext)
		
		val lookAngle = player.lookAngle
		
		if(!isFoil(stack)) {
			val track = state.block
			if(track is ITrackBlock && track.getTrackAxes(level, pos, state).size > 1) {
				if(!level.isClientSide) player.displayClientMessage(
					CreateLang.translateDirect("track.junction_start")
						.withStyle(ChatFormatting.RED), true
				)
				return InteractionResult.SUCCESS
			}
			
			val tbe = level.getBlockEntity(pos)
			if(tbe is TrackBlockEntity && tbe.isTilted) {
				if(!level.isClientSide) player.displayClientMessage(
					CreateLang.translateDirect("track.turn_start")
						.withStyle(ChatFormatting.RED), true
				)
				return InteractionResult.SUCCESS
			}
			
			if(selectFlexi(level, pos, lookAngle, stack)) {
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1f)
				return InteractionResult.SUCCESS
			}
			
			return super.useOn(pContext)
		} else if(player.isShiftKeyDown) {
			if(!level.isClientSide) {
				player.displayClientMessage(CreateLang.translateDirect("track.selection_cleared"), true)
				stack.remove(AllDataComponents.TrackConnectingFrom)
			} else {
				level.playSound(player, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.75f, 1f)
			}
			return InteractionResult.SUCCESS
		}
		
		val placing = state.block !is ITrackBlock
		val extend = stack.getOrDefault(CreateDataComponents.TRACK_EXTENDED_CURVE, false)
		stack.remove(CreateDataComponents.TRACK_EXTENDED_CURVE)
		
		if(placing) {
			if(!state.canBeReplaced()) pos = pos.relative(pContext.clickedFace)
			state = getPlacementState(pContext) ?: return InteractionResult.FAIL
		}
		
		val offhandItem = player.offhandItem
		val hasGirder = CreateBlocks.METAL_GIRDER.isIn(offhandItem)
		val info = FlexiTrackPlacement.tryConnect(level, player, pos, state, stack, hasGirder)
		
		when(info) {
			is FlexiPlaceResult.PlaceError -> {
				player.displayClientMessage(info.message, true)
				CreateSoundEvents.DENY.playFrom(player, 1f, 1f)
				return InteractionResult.FAIL
			}
			
			is FlexiPlacementInfo -> {}
		}
		
		
		if(level.isClientSide) return InteractionResult.SUCCESS
		
		stack = player.mainHandItem
		if(CreateTags.AllBlockTags.TRACKS.matches(stack)) {
			stack.remove(AllDataComponents.TrackConnectingFrom)
			stack.remove(CreateDataComponents.TRACK_CONNECTING_FROM)
			player.setItemInHand(pContext.hand, stack)
		}
		
		state.getSoundType(level, pos, null).let {
			level.playSound(
				null, pos, it.placeSound, SoundSource.BLOCKS,
				(it.getVolume() + 1.0f) / 2.0f, it.getPitch() * 0.8f
			)
		}
		
		return InteractionResult.SUCCESS
	}
	
	
	fun selectFlexi(world: LevelAccessor, pos: BlockPos, lookVec: Vec3, heldItem: ItemStack): Boolean {
		val blockState = world.getBlockState(pos)
		val block = blockState.block
		if(block !is ITrackBlock) return false
		
		val nearestTrackAxis = block.getNearestTrackAxis(world, pos, blockState, lookVec)
		val axis = nearestTrackAxis.getFirst()
			.scale((if(nearestTrackAxis.getSecond() == Direction.AxisDirection.POSITIVE) -1 else 1).toDouble())
		val normal = block.getUpNormal(world, pos, blockState).normalize()
		
		heldItem.set(AllDataComponents.TrackConnectingFrom, FlexiPlacementInfo.TrackPoint(pos, axis, normal))
		return true
	}
	
	override fun placeBlock(context: BlockPlaceContext, state: BlockState): Boolean =
		super.placeBlock(context, state).also { result ->
			val player = context.player ?: return result
			if(result) (context.level.getBlockEntity(context.clickedPos) as? FlexiTrackBlockEntity)?.let { be ->
				be.updateState(
					be.state.copy(baseShape = FlexiShape.Single(FlexiDirection.Known.roundFrom(vector = player.lookAngle)))
				)
			}
		}
	
	override fun isFoil(stack: ItemStack): Boolean =
		stack.has(AllDataComponents.TrackConnectingFrom)
}
