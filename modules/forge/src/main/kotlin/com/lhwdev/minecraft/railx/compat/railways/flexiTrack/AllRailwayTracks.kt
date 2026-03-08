@file:Suppress("UNCHECKED_CAST")

package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.lhwdev.minecraft.railx.compat.flexiTrack.CompatRailwayTracks
import com.railwayteam.railways.Railways
import com.railwayteam.railways.content.custom_tracks.gen_template.OutputPrefixer
import com.railwayteam.railways.registry.CRBlocks
import com.railwayteam.railways.registry.CRTrackMaterials
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
		CRBlocks.NARROW_GAUGE_TRACKS // does nothing but <cinit>
		
		fun blockStateModelOf(material: TrackMaterial) =
			Railways.asResource("${OutputPrefixer.DEFAULT.getOutputPrefix(material)}x_ortho")
		
		val materials = TrackMaterial.allFromMod(Railways.MOD_ID)
		Standard = materials.filter { it.trackType == TrackMaterial.TrackType.STANDARD }
			.flexiTrackBlocks(
				originalBlock = {
					Railways.registrate().get("track_" + it.resourceName(), Registries.BLOCK) as BlockEntry<TrackBlock>
				},
				factory = ::StandardFlexiTrackBlock,
				blockStateModel = ::blockStateModelOf,
			)
		
		WideGauge = CRTrackMaterials.WIDE_GAUGE.values.flexiTrackBlocks(
			originalBlock = { CRBlocks.WIDE_GAUGE_TRACKS[it] as BlockEntry<TrackBlock> },
			factory = ::WideGaugeFlexiTrackBlock,
			blockStateModel = ::blockStateModelOf,
		)
		
		NarrowGauge = CRTrackMaterials.NARROW_GAUGE.values.flexiTrackBlocks(
			originalBlock = { CRBlocks.NARROW_GAUGE_TRACKS[it] as BlockEntry<TrackBlock> },
			factory = ::NarrowGaugeFlexiTrackBlock,
			blockStateModel = ::blockStateModelOf,
		)
		
		Monorail = flexiTrackBlock(
			original = CRTrackMaterials.MONORAIL,
			originalBlock = CRBlocks.MONORAIL_TRACK,
			factory = ::MonorailFlexiTrackBlock,
			blockStateModel = Railways.asResource("block/monorail/monorail/static_blocks/x_ortho"),
		)
		
		Phantom = flexiTrackBlock(
			original = CRTrackMaterials.PHANTOM,
			originalBlock = CRBlocks.PHANTOM_TRACK,
			factory = ::PhantomFlexiTrackBlock,
			blockStateModel = blockStateModelOf(CRTrackMaterials.PHANTOM),
		)
		Universal = listOf(Phantom)
	}
}
