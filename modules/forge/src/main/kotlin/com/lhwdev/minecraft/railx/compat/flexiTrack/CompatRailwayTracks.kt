package com.lhwdev.minecraft.railx.compat.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.lhwdev.minecraft.railx.registry.RailXBlockEntityEntry
import com.lhwdev.minecraft.railx.registry.flexiTrackBlock
import com.lhwdev.minecraft.railx.registry.registrate.RailXRegistrate
import com.lhwdev.minecraft.railx.registry.registrate.RailXRegistry
import com.lhwdev.minecraft.railx.registry.registrate.validFor
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.neoforge.registries.DeferredHolder


abstract class CompatRailwayTracks {
	val Registry = RailXRegistry
	
	
	inline fun <Track : FlexiTrackBlock> flexiTrackBlock(
		original: TrackMaterial,
		originalBlock: DeferredHolder<Block, out TrackBlock>,
		blockEntity: RailXBlockEntityEntry<*> = AllBlockEntityTypes.FlexiTrack,
		crossinline factory: (BlockBehaviour.Properties, TrackMaterial) -> Track,
		blockStateModel: ResourceLocation,
		builder: BlockBuilder<Track, RailXRegistrate>.() -> Unit = {},
	): BlockEntry<Track> {
		val name = compatNameFor(original.id)
		Registry.registerCompatBlock(name, dependencyModId = original.id.namespace)
		
		return Registry.flexiTrackBlock(
			name = name,
			material = original,
			normalBlock = originalBlock,
			factory = factory,
			blockStates = { c, p -> p.simpleBlock(c.entry, p.models().getExistingFile(blockStateModel)) },
		) {
			validFor(blockEntity)
			builder()
		}
	}
	
	inline fun <Track : FlexiTrackBlock> Iterable<TrackMaterial>.flexiTrackBlocks(
		originalBlock: (TrackMaterial) -> DeferredHolder<Block, out TrackBlock>,
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
