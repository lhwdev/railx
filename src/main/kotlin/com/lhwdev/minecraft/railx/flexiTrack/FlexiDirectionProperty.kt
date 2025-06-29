package com.lhwdev.minecraft.railx.flexiTrack

import net.minecraft.world.level.block.state.properties.Property
import java.util.*


class FlexiDirectionProperty(name: String) :
	Property<FlexiDirection.FlatDivision>(name, FlexiDirection.FlatDivision::class.java) {
	override fun getName(value: FlexiDirection.FlatDivision): String =
		value.index.toString()
	
	override fun getPossibleValues(): Collection<FlexiDirection.FlatDivision> =
		FlexiDirection.FlatDivision.Divisions
	
	override fun getValue(value: String): Optional<FlexiDirection.FlatDivision> = try {
		val index = value.toInt()
		if(index >= 0 && index < FlexiDirection.FlatDivision.DivisionCount) {
			Optional.of(FlexiDirection.FlatDivision.Divisions[index])
		} else Optional.empty()
	} catch(_: NumberFormatException) {
		Optional.empty()
	}
	
	companion object {
		fun create(name: String): FlexiDirectionProperty = FlexiDirectionProperty(name)
	}
}
