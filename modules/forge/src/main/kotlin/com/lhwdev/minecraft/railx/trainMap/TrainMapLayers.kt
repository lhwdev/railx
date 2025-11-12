package com.lhwdev.minecraft.railx.trainMap

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import xaero.map.gui.GuiMap


class TrainMapLayers(map: GuiMap, val dimension: ResourceKey<Level>) :
	MapWidgetContainer<MapWidgetContainer.Entry<AbstractWidget>>(map) {
	var blocks = TrainMapBlocksRenderer(this, dimension, linearFiltering = false)
	
	init {
		addChildren(blocks)
	}
	
	override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
	}
}
