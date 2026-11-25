@file:Suppress("PropertyName", "ClassName")

package com.lhwdev.minecraft.railx.flywheel

import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackVisual


interface TrackVisualExtension {
	val `railx$visuals`: List<TrackVisual_BezierTrackVisualExtension>
}


interface TrackVisual_BezierTrackVisualExtension {
	@Deprecated(message = "invisible", level = DeprecationLevel.ERROR)
	val `railx$bc`: BezierConnection
}


inline val TrackVisual.visuals: List<TrackVisual_BezierTrackVisualExtension>
	get() = (this as TrackVisualExtension).`railx$visuals`

inline val TrackVisual_BezierTrackVisualExtension.bc: BezierConnection
	@Suppress("DEPRECATION_ERROR")
	get() = this.`railx$bc`
