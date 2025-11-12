package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.splitGraph.SplitGraphTrainSync
import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.network.syncher.EntityDataSerializer
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.*


@Suppress("unused")
object AllEntityDataSerializers {
	fun register() {}
	
	val Registry = RailXRegistry
	
	val SyncMergedGraph: RegistryEntry<EntityDataSerializer<*>, EntityDataSerializer<Optional<SplitGraphTrainSync.MergedInfo>>> =
		Registry.simple(
			"merged_track_graph",
			NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS
		) { SplitGraphTrainSync.MergedInfo.SERIALIZER }
}
