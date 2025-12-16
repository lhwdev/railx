package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.simibubi.create.Create
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrackScenes
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block


object AllPonderScenes {
	fun register(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
		val helper = helper.withKeyFunction<Holder<out Block>> { it.key!!.location() }
		
		helper.forComponents(FlexiTrackMaterial.allFlexiBlocks())
			.addStoryBoard(Create.asResource("train_track/placement"), TrackScenes::placement)
			.addStoryBoard(Create.asResource("train_track/portal"), TrackScenes::portal)
			.addStoryBoard(Create.asResource("train_track/chunks"), TrackScenes::chunks)
	}
}
