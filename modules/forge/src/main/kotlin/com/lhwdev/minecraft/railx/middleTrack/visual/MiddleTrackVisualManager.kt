package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.flywheel.VisualManagerType
import com.lhwdev.minecraft.railx.flywheel.VisualizationManagerContainer
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl
import net.minecraft.world.level.LevelAccessor


class MiddleTrackVisualManager : VisualManagerImpl<ConnectionMiddleState, MiddleTracksStorage>(MiddleTracksStorage()) {
	companion object {
		fun register() {
			VisualizationManagerContainer.ManagerTypes += MiddleTrackVisualManagerType
		}
		
		fun of(level: LevelAccessor): MiddleTrackVisualManager? =
			VisualizationManagerContainer.get(level)?.managers?.getManager(MiddleTrackVisualManagerType)
	}
	
	
	init {
		storage.manager = this
	}
	
}

private object MiddleTrackVisualManagerType :
	VisualManagerType<MiddleTrackVisualManager> {
	override fun create(): MiddleTrackVisualManager =
		MiddleTrackVisualManager()
}
