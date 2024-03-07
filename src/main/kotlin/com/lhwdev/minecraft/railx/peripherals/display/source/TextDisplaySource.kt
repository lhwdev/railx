package com.lhwdev.minecraft.railx.peripherals.display.source

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

class TextDisplaySource : DisplaySource() {
	override fun provideText(
		displayLinkContext: DisplayLinkContext,
		displayTargetStats: DisplayTargetStats,
	): List<MutableComponent> {
		/*
            Maybe some events in the future?
         */
		return (displayLinkContext.sourceBlockEntity as ComputerizedDisplaySourceBlockEntity)
			.getFromPos(displayLinkContext.blockEntity().blockPos)!!.toDisplay
	}
	
	override fun getPassiveRefreshTicks(): Int {
		return 20
	}
	
	companion object {
		val NIL_TEXT = Component.literal("")
	}
}
