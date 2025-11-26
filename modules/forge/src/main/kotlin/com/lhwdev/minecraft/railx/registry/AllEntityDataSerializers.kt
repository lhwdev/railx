package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.splitGraph.SplitGraphTrainSync
import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraftforge.registries.ForgeRegistries
import java.util.*


@Suppress("unused")
object AllEntityDataSerializers {
	fun register() {}
	
	val Registry = RailXRegistry
	
	val SyncMergedGraph: RegistryEntry<EntityDataSerializer<Optional<SplitGraphTrainSync.MergedInfo>>> =
		Registry.simple(
			"merged_track_graph",
			ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS
		) { SplitGraphTrainSync.MergedInfo.SERIALIZER }
}
