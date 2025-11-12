package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackRenderer
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVisual
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackBlockEntity
import com.simibubi.create.content.trains.observer.TrackObserverRenderer
import com.simibubi.create.content.trains.observer.TrackObserverVisual
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer


object AllBlockEntityTypes {
	fun register() {}
	
	val Registry = RailXRegistry
	
	val AdvancedTrackObserver: BlockEntityEntry<AdvancedTrackObserverBlockEntity> = Registry
		.blockEntity("advanced_track_observer", ::AdvancedTrackObserverBlockEntity)
		.visual { SimpleBlockEntityVisualizer.Factory(::TrackObserverVisual) }
		.renderer { NonNullFunction(::TrackObserverRenderer) }
		.validBlocks(AllBlocks.AdvancedTrackObserver)
		.register()
	
	val MiddleTrack: BlockEntityEntry<MiddleTrackBlockEntity> = Registry
		.blockEntity("middle_track", ::MiddleTrackBlockEntity)
		// .visual { SimpleBlockEntityVisualizer.Factory(::MiddleTrackVisual) }
		// .renderer { NonNullFunction(::MiddleTrackRenderer) }
		.validBlocks(AllBlocks.MiddleTrack)
		.register()
	
	val FlexiTrack: BlockEntityEntry<FlexiTrackBlockEntity> = Registry
		.blockEntity("flexi_track", ::FlexiTrackBlockEntity)
		.visual { SimpleBlockEntityVisualizer.Factory(::FlexiTrackVisual) }
		.renderer { NonNullFunction(::FlexiTrackRenderer) }
		.validBlocks(AllBlocks.FlexiTrack, AllBlocks.FlexiSplitGraphTrack)
		.register()
}
