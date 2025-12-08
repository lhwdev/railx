package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.middleTrack.visual.MiddleTrackVisuals
import com.lhwdev.minecraft.railx.utils.getOrNull
import com.simibubi.create.content.trains.track.TrackBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn


@OnlyIn(Dist.CLIENT)
object MiddleTrackClient {
	var LoadedTracks: Set<BlockPos> = emptySet()
		private set
	
	
	/**
	 * Note: called from mixin; called after `LevelRenderer.compileSections`
	 */
	fun tick() {
		val minecraft = Minecraft.getInstance()
		val level = minecraft.level ?: return
		
		if(RailXConfig.Server.middleTrack.enabled.getOrNull() != true) {
			if(LoadedTracks.isEmpty()) return
			
			LoadedTracks = emptySet()
			MiddleTrackVisuals.tickDisable(level)
			return
		}
		
		if(RailXConfig.Server.common.optimizeFakeTracks.isFalse) {
			minecraft.player?.sendSystemMessage(
				Component.literal(
					"railx: common.optimizeFakeTracks is turned off, but required for middleTrack feature. " +
						"turning it on..."
				)
			)
			RailXConfig.Server.common.optimizeFakeTracks.set(true)
		}
		
		val tracks = mutableSetOf<BlockPos>()
		minecraft.levelRenderer.iterateVisibleBlockEntities { be ->
			if(be.isRemoved) return@iterateVisibleBlockEntities
			if(be is TrackBlockEntity) tracks += be.blockPos
		}
		
		LoadedTracks = tracks
		MiddleTrackVisuals.tick(level)
	}
}
