package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackRenderer
import com.simibubi.create.content.trains.observer.TrackObserverRenderer
import com.simibubi.create.content.trains.observer.TrackObserverVisual
import com.simibubi.create.content.trains.track.TrackVisual
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer


object AllBlockEntityTypes {
	val Registry = RailXRegistry
	
	val AdvancedTrackObserver: BlockEntityEntry<AdvancedTrackObserverBlockEntity> = Registry
		.blockEntity("advanced_track_observer", ::AdvancedTrackObserverBlockEntity)
		.visual {
			SimpleBlockEntityVisualizer.Factory { ctx, blockEntity, partialTick ->
				TrackObserverVisual(ctx, blockEntity, partialTick)
			}
		}
		.renderer { NonNullFunction { TrackObserverRenderer(it) } }
		.validBlocks(AllBlocks.AdvancedTrackObserver)
		.register()
	
	val FlexiTrack: BlockEntityEntry<FlexiTrackBlockEntity> = Registry
		.blockEntity("track", ::FlexiTrackBlockEntity)
		.visual {
			SimpleBlockEntityVisualizer.Factory { ctx, blockEntity, partialTick ->
				TrackVisual(ctx, blockEntity, partialTick)
			}
		}
		.renderer { NonNullFunction { FlexiTrackRenderer(it) } }
		.validBlocks(AllBlocks.FlexiTrack)
		.register()
}
