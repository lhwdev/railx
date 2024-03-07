package com.lhwdev.minecraft.railx.peripherals.display.target

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import net.minecraft.network.chat.MutableComponent

class TextPassBehaviour : DisplayTarget() {
	override fun acceptText(line: Int, list: List<MutableComponent>, displayLinkContext: DisplayLinkContext) {
		val tile = displayLinkContext.targetBlockEntity as ComputerizedDisplayTargetBlockEntity
		tile.acceptText(line, list, displayLinkContext)
	}
	
	override fun provideStats(displayLinkContext: DisplayLinkContext): DisplayTargetStats {
		val tile = displayLinkContext.targetBlockEntity as ComputerizedDisplayTargetBlockEntity
		return DisplayTargetStats(tile.maxHeight, tile.maxHeight, this)
	}
}
