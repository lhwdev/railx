@file:Suppress("UNCHECKED_CAST")

package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.lhwdev.minecraft.railx.compat.flexiTrack.CompatRailwayTracks
import com.railwayteam.railways.Railways
import com.railwayteam.railways.registry.CRBlocks
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.AllTags
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.core.registries.Registries


object AllRailwayTracks : CompatRailwayTracks() {
	fun register() {}
	
	
	val Standard: List<BlockEntry<StandardFlexiTrackBlock>>
	
	val WideGauge: List<BlockEntry<WideGaugeFlexiTrackBlock>>
	
	val NarrowGauge: List<BlockEntry<NarrowGaugeFlexiTrackBlock>>
	
	val Monorail: BlockEntry<MonorailFlexiTrackBlock>
	
	val Universal: List<BlockEntry<out UniversalFlexiTrackBlock>>
	val Phantom: BlockEntry<PhantomFlexiTrackBlock>
	
	
	init {
		CRBlocks.register() // does nothing but <cinit>
		val railwayBlocks = Railways.registrate().getAll(Registries.BLOCK)
		val tracks = HashMap<String, BlockEntry<*>>()
		for(block in railwayBlocks) {
			val name = block.key.location().path
			if(!name.startsWith("track_")) continue
			if(block !is BlockEntry) continue
			tracks[name.substring(6)] = block // note: tracks also contain things like 'track_switch' 'track_coupler'
		}
		
		val materials = TrackMaterial.allFromMod(Railways.MOD_ID)
		Standard = materials.filter { it.trackType == TrackMaterial.TrackType.STANDARD }
			.flexiTrackBlocks(
				originalBlock = { tracks[it.resourceName()] as BlockEntry<TrackBlock> },
				factory = ::StandardFlexiTrackBlock
			)
		
		WideGauge = CRTrackMaterials.WIDE_GAUGE.values.flexiTrackBlocks(
			originalBlock = { CRBlocks.WIDE_GAUGE_TRACKS[it] as BlockEntry<TrackBlock> },
			factory = ::WideGaugeFlexiTrackBlock,
		)
		
		NarrowGauge = CRTrackMaterials.NARROW_GAUGE.values.flexiTrackBlocks(
			originalBlock = { CRBlocks.NARROW_GAUGE_TRACKS[it] as BlockEntry<TrackBlock> },
			factory = ::NarrowGaugeFlexiTrackBlock,
		)
		
		Monorail = flexiTrackBlock(
			original = CRTrackMaterials.MONORAIL,
			originalBlock = CRBlocks.MONORAIL_TRACK,
			factory = ::MonorailFlexiTrackBlock,
		)
		
		Phantom = flexiTrackBlock(
			original = CRTrackMaterials.PHANTOM,
			originalBlock = CRBlocks.PHANTOM_TRACK,
			factory = ::PhantomFlexiTrackBlock,
		)
		Universal = listOf(Phantom)
	}
}
