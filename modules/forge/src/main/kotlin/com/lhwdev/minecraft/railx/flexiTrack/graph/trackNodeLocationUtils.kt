@file:JvmName("TrackNodeLocationUtils")

package com.lhwdev.minecraft.railx.flexiTrack.graph

import com.simibubi.create.content.trains.graph.TrackNodeLocation


fun TrackNodeLocation.roundToOriginal() {
	(this as ITrackNodeLocation).setVecLocation(null)
}
