package com.lhwdev.minecraft.railx.flywheel

import dev.engine_room.flywheel.api.visualization.VisualizationManager
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl
import net.minecraft.world.level.LevelAccessor


@Suppress("NonExtendableApiUsage")
interface VisualizationManagerContainer : VisualizationManager, VisualizationManagerAccessor {
	val managers: VisualManagers
	
	
	companion object {
		val ManagerTypes: MutableList<VisualManagerType<*>> = mutableListOf()
		
		fun get(level: LevelAccessor): VisualizationManagerContainer? =
			VisualizationManager.get(level) as? VisualizationManagerContainer
	}
}

interface VisualManagerType<Manager : VisualManagerImpl<*, *>> {
	fun create(): Manager
}
