package com.lhwdev.minecraft.railx.realisticSpeed

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour


interface BlockPropertiesWithSource {
	var initialPropertiesSource: Block?
}


val BlockBehaviour.Properties.initialPropertiesSource: Block?
	get() = (this as BlockPropertiesWithSource).initialPropertiesSource
