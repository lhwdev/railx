package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackRenderer
import com.simibubi.create.content.trains.track.TrackRenderer
import com.simibubi.create.content.trains.track.TrackVisual
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer


object AllBlockEntityTypes {
	val Registry = FlexiRegistry
	
	val FlexiTrack: BlockEntityEntry<FlexiTrackBlockEntity> = Registry
		.blockEntity("track", ::FlexiTrackBlockEntity)
		.visual {
			SimpleBlockEntityVisualizer.Factory { ctx, blockEntity, partialTick ->
				TrackVisual(ctx, blockEntity, partialTick)
			}
		}
		.validBlocks(AllBlocks.FlexiTrack)
		.renderer { NonNullFunction { FlexiTrackRenderer(it) } }
		.register()
}
