package com.lhwdev.minecraft.railx.flexiTrack.mixin

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackPlacement
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.simibubi.create.AllDataComponents
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackPlacement
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState


object TrackBlockItemMixinHelper {
	fun tryConnectFromCreateTrackItem(
		level: Level, player: Player, pos2: BlockPos, state2: BlockState,
		stack: ItemStack, girder: Boolean, maximiseTurn: Boolean,
	): TrackPlacement.PlacementInfo? {
		val blockItem = stack.item as? BlockItem ?: return null
		if(blockItem !is FlexiTrackBlockItem) {
			val from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM) ?: return null
			if(level.getBlockState(from.pos).block !is FlexiTrackBlock) return null
		}
		
		val result = FlexiTrackPlacement.tryConnect(level, player, pos2, state2, stack, girder)
		val info: PlacementInfoAccessor
		@Suppress("KotlinConstantConditions")
		if(result is FlexiTrackPlacement.FlexiPlacementInfo) {
			info = TrackPlacement.PlacementInfo(result.material) as PlacementInfoAccessor
			result.error?.let {error ->
				info.message = error.message.toString()
				info.valid = false
			}
		} else {
			info = TrackPlacement.PlacementInfo(TrackMaterial.ANDESITE) as PlacementInfoAccessor
		}
		return info as TrackPlacement.PlacementInfo
	}
}