@file:JvmName("TrackNodeLocationUtils")

package com.lhwdev.minecraft.railx.flexiTrack.graph

import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.round


fun TrackNodeLocation.roundToOriginal() {
	(this as ITrackNodeLocation).setVecLocation(null)
}


fun Vec3.isIntTrackNodeLocation(): Boolean {
	val x = x * 2.0
	val z = z * 2.0
	return round(x) similarTo x &&
		floor(y) similarTo y &&
		round(z) similarTo z
}
