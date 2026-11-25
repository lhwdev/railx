package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flywheel.VisualManagerExtension
import com.lhwdev.minecraft.railx.flywheel.VisualManagerType
import com.lhwdev.minecraft.railx.flywheel.VisualizationManagerContainer
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.lhwdev.minecraft.railx.middleTrack.GlobalConnections
import dev.engine_room.flywheel.api.task.Plan
import dev.engine_room.flywheel.api.visual.DynamicVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage
import dev.engine_room.flywheel.lib.task.SimplePlan
import dev.engine_room.flywheel.lib.task.functional.RunnableWithContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.LevelAccessor


class MiddleTrackVisualManager(primaryTracks: BlockEntityStorage) :
	VisualManagerImpl<ConnectionMiddleState, MiddleTracksStorage>(MiddleTracksStorage(primaryTracks)),
	VisualManagerExtension {
	companion object {
		fun register() {
			VisualizationManagerContainer.ManagerTypes += MiddleTrackVisualManagerType
		}
	}
	
	
	private var previousConnections = emptySet<ConnectionMiddleState>()
	
	override fun beforeFrame(level: LevelAccessor) {
		if(level !is ClientLevel) return
		
		val connections = if(RailXConfig.Server.middleTrack.enabled.isTrue) {
			GlobalConnections[level].filterTo(HashSet()) { it.canRender }
		} else {
			emptySet()
		}
		
		for(connection in previousConnections) {
			if(connection !in connections) queueRemove(connection)
		}
		for(connection in connections) {
			if(connection !in previousConnections) queueAdd(connection)
		}
		previousConnections = connections
	}
	
	override fun framePlan(context: VisualizationContext): Plan<DynamicVisual.Context> =
		super.framePlan(context)
			.then(SimplePlan.of(RunnableWithContext.Ignored(::syncAll)))
	
	override fun recreate(context: VisualizationContext, partialTick: Float) {
		super.recreate(context, partialTick)
		processQueue(context, partialTick)
		syncAll()
	}
	
	private fun syncAll() {
		for(visual in storage.allVisuals) when(visual) {
			is MiddleTrackVisual -> visual.sync()
		}
	}
}

private object MiddleTrackVisualManagerType :
	VisualManagerType<MiddleTrackVisualManager> {
	override fun create(container: VisualizationManagerContainer): MiddleTrackVisualManager =
		MiddleTrackVisualManager(container.blockEntities.storage)
}
