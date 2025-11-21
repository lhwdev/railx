package com.lhwdev.minecraft.railx.utils

import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.entity.TravellingPoint


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
