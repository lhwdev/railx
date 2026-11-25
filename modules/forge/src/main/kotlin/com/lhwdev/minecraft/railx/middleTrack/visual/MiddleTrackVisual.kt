package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.common.rendering.CommonTrackVisual
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.simibubi.create.content.trains.track.BezierConnection
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import net.createmod.catnip.data.Couple


class MiddleTrackVisual(
	context: VisualizationContext,
	private val connection: ConnectionMiddleState,
	private val willRender: (BezierConnection) -> Boolean,
) : CommonTrackVisual(context) {
	private var bezier: Bezier? = null
	private var observedCurve: BezierConnection? = null
	
	override val beziers: Collection<Bezier>
		get() = listOfNotNull(bezier)
	
	fun sync() {
		val curve = connection.curve
		val bezier = this.bezier
		if(!willRender(curve)) {
			if(bezier == null) return
			bezier.delete()
			this.bezier = null
			observedCurve = null
		} else {
			if(curve === observedCurve) return
			observedCurve = curve
			
			if(bezier?.curve?.hasSameRenderingInputs(curve) == true) return
			
			bezier?.delete()
			if(bezier != null) println("??? why geometry changed")
			this.bezier = Bezier(curve)
			observedCurve = curve
		}
		
		update()
	}
	
	override fun delete() {
		super.delete()
		bezier = null
		observedCurve = null
	}
}

private fun BezierConnection.hasSameRenderingInputs(other: BezierConnection): Boolean =
	material == other.material &&
		hasGirder == other.hasGirder &&
		bePositions.hasSameValues(other.bePositions) &&
		starts.hasSameValues(other.starts) &&
		axes.hasSameValues(other.axes) &&
		normals.hasSameValues(other.normals)

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> Couple<T>.hasSameValues(other: Couple<T>): Boolean =
	first == other.first && second == other.second
