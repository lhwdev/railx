package com.lhwdev.minecraft.railx.flexiTrak

import net.minecraft.world.level.block.state.properties.Property
import java.util.*


sealed class FlexiShape(val ordinal: Int) : Comparable<FlexiShape> {
	override fun compareTo(other: FlexiShape): Int = ordinal - other.ordinal
	
	
	companion object {
	
	}
	
	
	object None : FlexiShape(ordinal = 0)
	
	sealed class Single(ordinal: Int) : FlexiShape(ordinal)
	
	class Simple(val direction: FlexiDirection) : Single(direction.ordinal)
	
	class DiamondCrossing : FlexiShape()
	
	class SquareCrossing : FlexiShape()
	
	class Portal : Single()
}


object FlexiShapeProperty : Property<FlexiShape>("flexi_shape", FlexiShape::class.java) {
	override fun getName(value: FlexiShape): String = "${value.ordinal}"
	
	override fun getPossibleValues(): MutableCollection<FlexiShape> {
		TODO("Not yet implemented")
	}
	
	override fun getValue(p_61701_: String): Optional<FlexiShape> {
		TODO("Not yet implemented")
	}
}
