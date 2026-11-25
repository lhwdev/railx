package com.lhwdev.minecraft.railx.flywheel

import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl
import net.minecraft.world.level.LevelAccessor


class VisualManagers(self: VisualizationManagerContainer) {
	private val map = VisualizationManagerContainer.ManagerTypes.associateWith { it.create(self) }
	
	val value: Collection<VisualManagerImpl<*, *>> = map.values
	
	
	@Suppress("UNCHECKED_CAST")
	fun <Manager : VisualManagerImpl<*, *>> getManager(type: VisualManagerType<Manager>): Manager =
		map.getValue(type) as Manager
	
	fun attachVisualizationContext(context: VisualizationContext) {
		for(manager in value) {
			if(manager is ContextAwareVisualManager) manager.attachContext(context)
		}
	}
	
	fun beforeFrame(level: LevelAccessor) {
		for(manager in value) {
			if(manager is VisualManagerExtension) manager.beforeFrame(level)
		}
	}
}
