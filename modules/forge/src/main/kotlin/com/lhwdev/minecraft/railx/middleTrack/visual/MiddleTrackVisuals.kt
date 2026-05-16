package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.lhwdev.minecraft.railx.middleTrack.GlobalConnections
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackClient
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn


@OnlyIn(Dist.CLIENT)
object MiddleTrackVisuals {
	private class State {
		var previousLoadedTracks = emptySet<BlockPos>()
		var previousMiddles = emptySet<ConnectionMiddleState>()
		
		fun clear() {
			previousLoadedTracks = emptySet()
			previousMiddles = emptySet()
		}
	}
	
	private val state = WorldAttached { State() }
	
	fun tick(level: Level) {
		if(!VisualizationManager.supportsVisualization(level)) {
			state[level].clear()
			return
		}
		val manager = MiddleTrackVisualManager.of(level) ?: return
		
		val state = state[level]
		val connections = GlobalConnections[level]
		val middles = connections.toSet()
		for(added in middles - state.previousMiddles) {
			manager.queueAdd(added)
			// manager.queueUpdate(added)
		}
		for(removed in state.previousMiddles - middles) manager.queueRemove(removed)
		state.previousMiddles = middles
		
		val loadedTracks = MiddleTrackClient.LoadedTracks[level]
		for(added in loadedTracks - state.previousLoadedTracks)
			connections.getAll(added).forEach(manager::queueUpdate)
		for(removed in state.previousLoadedTracks - loadedTracks)
			connections.getAll(removed).forEach(manager::queueUpdate)
		state.previousLoadedTracks = loadedTracks
	}
	
	fun tickDisable(level: LevelAccessor) {
		if(!VisualizationManager.supportsVisualization(level)) return
		val manager = MiddleTrackVisualManager.of(level) ?: return
		val state = state[level]
		
		for(middle in state.previousMiddles) manager.queueRemove(middle)
		state.previousLoadedTracks = emptySet()
		state.previousMiddles = emptySet()
	}
}
