package com.lhwdev.minecraft.railx.flexiTrack

import net.minecraft.world.level.block.state.properties.Property
import java.util.*


class FlexiDirectionProperty(name: String) :
	Property<FlexiDirection.Known>(name, FlexiDirection.Known::class.java) {
	override fun getName(value: FlexiDirection.Known): String =
		value.index.toString()
	
	override fun getPossibleValues(): Collection<FlexiDirection.Known> =
		FlexiDirection.Known.Divisions
	
	override fun getValue(value: String): Optional<FlexiDirection.Known> = try {
		val index = value.toInt()
		if(index >= 0 && index < FlexiDirection.Known.DivisionCount) {
			Optional.of(FlexiDirection.Known.Divisions[index])
		} else Optional.empty()
	} catch(_: NumberFormatException) {
		Optional.empty()
	}
	
	companion object {
		fun create(name: String): FlexiDirectionProperty = FlexiDirectionProperty(name)
	}
}
