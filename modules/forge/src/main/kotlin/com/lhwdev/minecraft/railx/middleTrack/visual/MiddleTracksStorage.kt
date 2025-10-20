package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.impl.visualization.storage.Storage


class MiddleTracksStorage : Storage<ConnectionMiddleState>() {
	internal lateinit var manager: MiddleTrackVisualManager
	
	override fun willAccept(obj: ConnectionMiddleState): Boolean =
		true
	
	override fun createRaw(
		visualizationContext: VisualizationContext,
		obj: ConnectionMiddleState,
		partialTick: Float,
	): MiddleTrackVisual = MiddleTrackVisual(context = visualizationContext, connection = obj)
}
