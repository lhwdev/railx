package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.peripherals.display.source.TextDisplaySource
import com.lhwdev.minecraft.railx.peripherals.display.target.TextPassBehaviour
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours
import net.minecraft.resources.ResourceLocation


fun registerBehaviors() {
	AllDisplayBehaviours.assignBlockEntity(
		AllDisplayBehaviours.register(
			ResourceLocation(
				RailX.modId,
				"computerized_display_source"
			),
			TextDisplaySource()
		),
		Registries.COMPUTERIZED_DISPLAY_SOURCE_BLOCK_ENTITY.get()
	)
	
	AllDisplayBehaviours.assignBlockEntity(
		AllDisplayBehaviours.register(
			ResourceLocation(
				RailX.modId,
				"computerized_display_target"
			),
			TextPassBehaviour()
		),
		Registries.COMPUTERIZED_DISPLAY_TARGET_BLOCK_ENTITY.get()
	)
}
