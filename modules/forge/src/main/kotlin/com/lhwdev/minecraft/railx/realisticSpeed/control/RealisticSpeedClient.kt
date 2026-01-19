package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn


@OnlyIn(Dist.CLIENT)
object RealisticSpeedClient {
	var parameters: RealisticSpeedParameters? = null
		get() {
			val value = field
			if(value != null && ControlsHandler.getControlsPos() == null) {
				field = null
				return null
			}
			return value
		}
}
