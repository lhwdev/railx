@file:Suppress("NonJavaMixin")

package com.lhwdev.minecraft.railx.mixin.facade

import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train.TrainEventHub
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.entity.TrainStatus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject


@Mixin(TrainStatus::class)
class TrainStatusMixin {
	private class Item(val key: String, val isGood: Boolean, val args: List<Any?>)
	
	
	@Shadow
	lateinit var train: Train
	
	@Unique
	private val dataQueue = mutableListOf<Item>()
	
	
	@Inject(at = [At("HEAD")])
	fun displayInformation(key: String, itsAGoodThing: Boolean, vararg args: Any?) {
		dataQueue += Item(key, itsAGoodThing, listOf(*args))
	}
	
	@Inject(at = [At("HEAD")])
	fun tick() {
		val eventHub = TrainEventHub.get(train)
		dataQueue.forEach {
			eventHub.onTrainStatus(it.key, it.isGood, *it.args.toTypedArray())
		}
		dataQueue.clear()
	}
}
