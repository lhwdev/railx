package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.common.rendering.CommonTrackVisual
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import dev.engine_room.flywheel.api.visualization.VisualizationContext


class MiddleTrackVisual(context: VisualizationContext, val connection: ConnectionMiddleState) :
	CommonTrackVisual(context, curve = connection.curve) {
	
	var bezier: Bezier? = null
	
	init {
		update(0f)
	}
	
	
	override fun update(partialTick: Float) {
		super.update(partialTick)
		if(connection.isActive) {
			if(bezier == null) bezier = Bezier(curve)
		} else {
			bezier?.delete()
			bezier = null
		}
	}
	
	override fun delete() {
		super.delete()
		bezier?.delete()
	}
}
