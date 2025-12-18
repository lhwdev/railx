package com.lhwdev.minecraft.railx.compat.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.registry.*
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockBehaviour


abstract class CompatRailwayTracks {
	val Registry = RailXRegistry
	
	
	inline fun <Track : FlexiTrackBlock> flexiTrackBlock(
		original: TrackMaterial,
		originalBlock: RegistryEntry<out TrackBlock>,
		blockEntity: RailXBlockEntityEntry<*> = AllBlockEntityTypes.FlexiTrack,
		crossinline factory: (BlockBehaviour.Properties, TrackMaterial) -> Track,
		blockStateModel: ResourceLocation,
		builder: BlockBuilder<Track, RailXRegistrate>.() -> Unit = {},
	): BlockEntry<Track> = Registry.flexiTrackBlock(
		name = compatNameFor(original.id),
		material = original,
		normalBlock = originalBlock,
		factory = factory,
		blockStates = { c, p -> p.simpleBlock(c.entry, p.models().getExistingFile(blockStateModel)) },
	) {
		validFor(blockEntity)
		builder()
	}
	
	inline fun <Track : FlexiTrackBlock> Iterable<TrackMaterial>.flexiTrackBlocks(
		originalBlock: (TrackMaterial) -> RegistryEntry<out TrackBlock>,
		blockEntity: RailXBlockEntityEntry<*> = AllBlockEntityTypes.FlexiTrack,
		crossinline factory: (BlockBehaviour.Properties, TrackMaterial) -> Track,
		blockStateModel: (TrackMaterial) -> ResourceLocation,
		builder: BlockBuilder<Track, RailXRegistrate>.() -> Unit = {},
	): List<BlockEntry<Track>> = map {
		flexiTrackBlock(it, originalBlock(it), blockEntity, factory, blockStateModel(it), builder)
	}
}


fun compatNameFor(location: ResourceLocation): String {
	return "${location.namespace}__track_${location.path}"
}
