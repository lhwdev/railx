package com.lhwdev.minecraft.railx.common.carriageTilt

import com.simibubi.create.content.trains.entity.Carriage
import net.createmod.catnip.data.Couple

interface DimensionalCarriageEntityWithTilt {
	val `railx$tilt`: Couple<Double?>
}

val Carriage.DimensionalCarriageEntity.tilt: Couple<Double?>
	get() = (this as DimensionalCarriageEntityWithTilt).`railx$tilt`
