package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.registry.AllKeys
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockItem
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackPlacement
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.network.PacketDistributor


object FlexiTrackBlockItem {
	val StubPlacementInfo: TrackPlacement.PlacementInfo =
		TrackPlacement.PlacementInfo(TrackMaterial.ANDESITE).also {
			@Suppress("KotlinConstantConditions")
			(it as PlacementInfoAccessor).valid = true
		}
	
	fun getFlexiblePlacementState(item: TrackBlockItem, context: BlockPlaceContext): BlockState? =
		FlexiTrackMaterial.maybeFlexible((item.block as ITrackBlock).material).get().getStateForPlacement(context)
	
	fun sendFlexiblePlacementPacket(event: PlayerInteractEvent.RightClickBlock) {
		val stack = event.itemStack
		if(!event.level.isClientSide) return
		if(!com.simibubi.create.AllTags.AllBlockTags.TRACKS.matches(stack)) return
		val packet = FlexiblePlacementPacket(
			mainHand = event.hand == InteractionHand.MAIN_HAND,
			flexible = AllKeys.FlexiblePlacement.isPressed
		)
		PacketDistributor.sendToServer(packet)
	}
	
	fun placeBlock(context: BlockPlaceContext, state: BlockState) {
		if(state.block !is FlexiTrackBlock) return
		val be = context.level.getBlockEntity(context.clickedPos) as? FlexiTrackBlockEntity ?: return
		val player = context.player ?: return
		val direction = FlexiDirection.Known.roundFrom(player.lookAngle)
		be.updateState(be.state.copy(baseShape = FlexiShape.Single(direction)))
	}
}
