package com.lhwdev.minecraft.railx.realisticSpeed

import com.simibubi.create.content.trains.entity.Carriage
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level


@Suppress("PropertyName")
interface ICarriage {
	val `railx$entities`: Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity>
}


val Carriage.entities: Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity>
	get() = (this as ICarriage).`railx$entities`
