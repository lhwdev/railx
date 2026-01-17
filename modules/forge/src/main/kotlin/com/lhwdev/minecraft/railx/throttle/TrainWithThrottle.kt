@file:JvmName("TrainThrottleUtils")

package com.lhwdev.minecraft.railx.throttle

import com.simibubi.create.content.trains.entity.Train


@Suppress("PropertyName")
interface TrainWithThrottle {
	var `railx$throttle`: Throttles.Throttle
}


var Train.absoluteThrottle: Throttles.Throttle
	get() = (this as TrainWithThrottle).`railx$throttle`
	set(value) {
		(this as TrainWithThrottle).`railx$throttle` = value
	}

fun Train.relativeThrottle(forward: Boolean): Throttles.Throttle =
	absoluteThrottle.reverseIf(forward = forward)
