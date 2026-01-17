package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.lhwdev.minecraft.railx.realisticSpeed.RealisticTrainSpeed
import com.lhwdev.minecraft.railx.realisticSpeed.realisticSpeed
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec


data class RealisticSpeedParameters(
	val slip: Boolean,
) {
	constructor(realistic: RealisticTrainSpeed) : this(
		slip = realistic.slipAmount != 0.0,
	)
	
	companion object {
		val streamCodec = StreamCodec.composite(
			ByteBufCodecs.BOOL, RealisticSpeedParameters::slip,
			::RealisticSpeedParameters,
		)
		
		operator fun invoke(control: RealisticSpeedControl.Control): RealisticSpeedParameters? =
			control.train.realisticSpeed?.let { RealisticSpeedParameters(it) }
	}
}
