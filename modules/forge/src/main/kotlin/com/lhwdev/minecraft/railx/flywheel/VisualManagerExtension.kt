package com.lhwdev.minecraft.railx.flywheel

import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl
import net.minecraft.world.level.LevelAccessor


interface VisualManagerExtension {
	/**
	 * Runs on the render thread before Flywheel executes the frame plan.
	 */
	fun beforeFrame(level: LevelAccessor) {}
	
	fun recreate(context: VisualizationContext, partialTick: Float) {
		(this as VisualManagerImpl<*, *>).storage.recreateAll(context, partialTick)
	}
}
