package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState


fun TrackMaterial.defaultBlockState(): BlockState =
	block.defaultBlockState()


private typealias TrackReference = RegistryEntry<out TrackBlock>
private typealias FlexiTrackReference = RegistryEntry<out FlexiTrackBlock>

object FlexiTrackMaterial {
	val ToFlexible = HashMap<ResourceLocation, FlexiTrackReference>()
	val ToNormal = HashMap<ResourceLocation, TrackReference>()
	
	
	fun addMaterial(original: TrackReference, flexiTrack: FlexiTrackReference) {
		ToFlexible[original.key.location()] = flexiTrack
		ToNormal[flexiTrack.key.location()] = original
	}
	
	fun allFlexiBlocks(): Collection<FlexiTrackReference> =
		ToFlexible.values
	
	fun <T> toFlexible(block: T): FlexiTrackBlock? where T : ITrackBlock, T : Block = block as? FlexiTrackBlock
		?: @Suppress("DEPRECATION") ToFlexible[block.builtInRegistryHolder().unwrapKey().get().location()]?.get()
	
	fun maybeFlexible(block: Block): Block =
		if(block is ITrackBlock) toFlexible(block) ?: block else block
}
