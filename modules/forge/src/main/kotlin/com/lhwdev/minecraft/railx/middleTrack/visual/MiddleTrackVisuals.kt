package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.lhwdev.minecraft.railx.middleTrack.GlobalConnections
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackClient
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn


@OnlyIn(Dist.CLIENT)
object MiddleTrackVisuals {
	private var previousLoadedTracks = emptySet<BlockPos>()
	private var previousMiddles = emptySet<ConnectionMiddleState>()
	
	fun tick(level: Level) {
		if(!VisualizationManager.supportsVisualization(level)) {
			previousLoadedTracks = emptySet()
			previousMiddles = emptySet()
			return
		}
		val manager = MiddleTrackVisualManager.of(level) ?: return
		
		val connections = GlobalConnections[level]
		val middles = connections.toSet()
		for(added in middles - previousMiddles) {
			manager.queueAdd(added)
			// manager.queueUpdate(added)
		}
		for(removed in previousMiddles - middles) manager.queueRemove(removed)
		previousMiddles = middles
		
		val loadedTracks = MiddleTrackClient.LoadedTracks
		for(added in loadedTracks - previousLoadedTracks) connections.getAll(added).forEach(manager::queueUpdate)
		for(removed in previousLoadedTracks - loadedTracks) connections.getAll(removed).forEach(manager::queueUpdate)
		previousLoadedTracks = loadedTracks
	}
	
	fun tickDisable(level: LevelAccessor) {
		if(!VisualizationManager.supportsVisualization(level)) return
		val manager = MiddleTrackVisualManager.of(level) ?: return
		for(middle in previousMiddles) manager.queueRemove(middle)
		previousLoadedTracks = emptySet()
		previousMiddles = emptySet()
	}
}
