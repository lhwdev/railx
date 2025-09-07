package com.lhwdev.minecraft.railx.utils

import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.instance.Instancer


fun <I : Instance> Instancer<I>.createInstances(size: Int): List<I> =
	List(size) { createInstance() }
