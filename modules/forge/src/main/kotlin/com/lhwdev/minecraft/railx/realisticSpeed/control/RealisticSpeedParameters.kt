package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.lhwdev.minecraft.railx.realisticSpeed.RealisticTrainSpeed
import com.lhwdev.minecraft.railx.realisticSpeed.realisticSpeed
import net.minecraft.network.FriendlyByteBuf


data class RealisticSpeedParameters(
	val slip: Boolean,
) {
	constructor(realistic: RealisticTrainSpeed) : this(
		slip = realistic.slipAmount != 0.0,
	)
	
	fun write(buffer: FriendlyByteBuf) {
		buffer.writeBoolean(slip)
	}
	
	
	companion object {
		fun read(buffer: FriendlyByteBuf): RealisticSpeedParameters = RealisticSpeedParameters(
			slip = buffer.readBoolean(),
		)
		
		operator fun invoke(control: RealisticSpeedControl.Control): RealisticSpeedParameters? =
			control.train.realisticSpeed?.let { RealisticSpeedParameters(it) }
	}
}
