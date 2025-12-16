package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Supplier


fun TrackMaterial.defaultBlockState(): BlockState =
	block.defaultBlockState()


private typealias FlexiTrackReference = DeferredHolder<Block, out FlexiTrackBlock>

object FlexiTrackMaterial {
	val ToFlexible = Reference2ObjectOpenHashMap<TrackMaterial, FlexiTrackReference>()
	
	init {
		ToFlexible[TrackMaterial.ANDESITE] = AllBlocks.FlexiTrack
	}
	
	
	fun addMaterial(block: FlexiTrackReference, material: TrackMaterial) {
		ToFlexible[material] = block
	}
	
	fun allFlexiBlocks(): Collection<FlexiTrackReference> =
		ToFlexible.values
	
	fun toFlexible(material: TrackMaterial): Supplier<out FlexiTrackBlock>? =
		ToFlexible[material]
	
	fun maybeFlexible(material: TrackMaterial): Supplier<out TrackBlock> =
		toFlexible(material) ?: material.blockSupplier
}
