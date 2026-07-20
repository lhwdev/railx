package com.lhwdev.minecraft.railx.utils

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.track.TrackBlockOutline


val Train.allTravellingPoints: List<TravellingPoint>
	get() = buildList {
		for(carriage in carriages) {
			add(carriage.leadingBogey().leading())
			add(carriage.leadingBogey().trailing())
			if(!carriage.isOnTwoBogeys) continue
			add(carriage.trailingBogey().leading())
			add(carriage.trailingBogey().trailing())
		}
	}

val TrackBlockOutline.BezierPointSelection.flexiDirection: FlexiDirection
	get() {
		val bc = blockEntity.connections[loc.curveTarget]!!
		val t = bc.getSegmentT(loc.segment)
		val normal = bc.getNormal(t.toDouble())
		return FlexiDirection.Two(tangent = direction.normalize(), normal = normal.normalize())
			.optimize()
	}
