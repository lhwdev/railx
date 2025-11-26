package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.simibubi.create.Create
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrackScenes
import com.tterrag.registrate.util.entry.RegistryEntry
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.minecraft.resources.ResourceLocation


object AllPonderScenes {
	fun register(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
		val helper = helper.withKeyFunction(RegistryEntry<*>::getId)
		
		helper.forComponents(
			FlexiTrackMaterial.allFlexiBlocks()
				.filterIsInstance<RegistryEntry<*>>()
		)
			.addStoryBoard(Create.asResource("train_track/placement"), TrackScenes::placement)
			.addStoryBoard(Create.asResource("train_track/portal"), TrackScenes::portal)
			.addStoryBoard(Create.asResource("train_track/chunks"), TrackScenes::chunks)
	}
}
