package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper
import net.minecraft.resources.ResourceLocation


class RailXPonderPlugin : PonderPlugin {
	override fun getModId(): String = RailX.Id
	
	override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
		AllPonderScenes.register(helper)
	}
	
	override fun registerTags(helper: PonderTagRegistrationHelper<ResourceLocation>) {
		AllPonderTags.register(helper)
	}
}
