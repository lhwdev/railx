package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train

import com.simibubi.create.content.trains.entity.Train


interface TrainEventHub {
	companion object {
		fun get(train: Train): TrainEventHub = TODO()
	}
	
	
	/**
	 * Also sends event to train owner.
	 */
	fun onTrainStatus(name: String, vararg args: Any?)
}
