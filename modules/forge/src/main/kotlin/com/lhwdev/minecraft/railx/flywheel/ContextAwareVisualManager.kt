package com.lhwdev.minecraft.railx.flywheel

import dev.engine_room.flywheel.api.visualization.VisualizationContext


interface ContextAwareVisualManager {
	fun attachContext(context: VisualizationContext)
}
