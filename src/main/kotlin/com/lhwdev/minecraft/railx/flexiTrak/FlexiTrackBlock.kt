package com.lhwdev.minecraft.railx.flexiTrak

import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.IntegerProperty


class FlexiTrackBlock(properties: Properties, material: TrackMaterial) : Block(properties), ITrackBlock {
	companion object {
		val Shape = IntegerProperty.create("shape", 0, 255)
	}
	
	
	init {
		registerDefaultState(
			defaultBlockState()
				.setValue()
		)
	}
}
