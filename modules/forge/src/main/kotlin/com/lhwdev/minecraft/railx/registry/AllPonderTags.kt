package com.lhwdev.minecraft.railx.registry

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags
import com.tterrag.registrate.util.entry.RegistryEntry
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper
import net.minecraft.resources.ResourceLocation


object AllPonderTags {
	fun register(helper: PonderTagRegistrationHelper<ResourceLocation>) {
		val helper = helper.withKeyFunction(RegistryEntry<*>::getId)
		helper.addToTag(AllCreatePonderTags.TRAIN_RELATED)
			.add(AllBlocks.FlexiTrack)
	}
}
