package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.middleTrack.visual.MiddleTrackVisuals
import com.lhwdev.minecraft.railx.utils.getOrNull
import com.simibubi.create.content.trains.track.TrackBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn


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
		
		val tracks = mutableSetOf<BlockPos>()
		// Assuming every TrackBlockEntity is globalBlockEntities, as TrackRenderer.shouldRenderOffScreen returns true
		minecraft.levelRenderer.globalBlockEntities.forEach { be ->
			if(be.isRemoved) return@forEach
			if(be is TrackBlockEntity) tracks += be.blockPos
		}
		
		LoadedTracks = tracks
		MiddleTrackVisuals.tick(level)
	}
}
