package com.lhwdev.minecraft.railx.flywheel

import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl


class VisualManagers(self: VisualizationManagerContainer) {
	private val map = VisualizationManagerContainer.ManagerTypes.associateWith { it.create() }
	
	val value: Collection<VisualManagerImpl<*, *>> = map.values
	
	val all: List<VisualManagerImpl<*, *>> = mutableListOf(self.blockEntities, self.entities, self.effects)
		.also { it += value }
	
	
	@Suppress("UNCHECKED_CAST")
	fun <Manager : VisualManagerImpl<*, *>> getManager(type: VisualManagerType<Manager>): Manager =
		map.getValue(type) as Manager
	
	fun attachVisualizationContext(context: VisualizationContext) {
		for(manager in value) {
			if(manager is ContextAwareVisualManager) manager.attachContext(context)
		}
	}
}
