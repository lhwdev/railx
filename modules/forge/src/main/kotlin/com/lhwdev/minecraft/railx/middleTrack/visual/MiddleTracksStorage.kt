package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.common.from
import com.lhwdev.minecraft.railx.flywheel.bc
import com.lhwdev.minecraft.railx.flywheel.visuals
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.simibubi.create.content.trains.track.TrackVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage
import dev.engine_room.flywheel.impl.visualization.storage.Storage


class MiddleTracksStorage(private val blockEntities: BlockEntityStorage) : Storage<ConnectionMiddleState>() {
	override fun willAccept(obj: ConnectionMiddleState): Boolean =
		true
	
	override fun createRaw(
		visualizationContext: VisualizationContext,
		obj: ConnectionMiddleState,
		partialTick: Float,
	): MiddleTrackVisual = MiddleTrackVisual(
		visualizationContext,
		connection = obj,
		willRender = { curve ->
			val visual = blockEntities.visualAtPos(curve.from.asLong()) as? TrackVisual
			val alreadyRendered = visual != null && visual.visuals.any { it.bc.key == curve.key }
			!alreadyRendered
		}
	)
}
