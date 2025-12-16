package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackRenderer
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVisual
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackBlockEntity
import com.simibubi.create.content.trains.observer.TrackObserverRenderer
import com.simibubi.create.content.trains.observer.TrackObserverVisual
import com.tterrag.registrate.util.nullness.NonNullFunction
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer


object AllBlockEntityTypes {
	fun register() {}
	
	val Registry = RailXRegistry
	
	val AdvancedTrackObserver: RailXBlockEntityEntry<AdvancedTrackObserverBlockEntity> = Registry.blockEntity(
		name = "advanced_track_observer",
		factory = ::AdvancedTrackObserverBlockEntity
	) {
		visual { SimpleBlockEntityVisualizer.Factory(::TrackObserverVisual) }
		renderer { NonNullFunction(::TrackObserverRenderer) }
		validBlocks(AllBlocks.AdvancedTrackObserver)
	}
	
	val MiddleTrack: RailXBlockEntityEntry<MiddleTrackBlockEntity> = Registry.blockEntity(
		name = "middle_track",
		factory = ::MiddleTrackBlockEntity
	) {
		// visual { SimpleBlockEntityVisualizer.Factory(::MiddleTrackVisual) }
		// renderer { NonNullFunction(::MiddleTrackRenderer) }
		validBlocks(AllBlocks.MiddleTrack)
	}
	
	val FlexiTrack: RailXBlockEntityEntry<FlexiTrackBlockEntity> = Registry.blockEntity(
		name = "flexi_track",
		factory = ::FlexiTrackBlockEntity
	) {
		validBlocks(AllBlocks.FlexiTrack, AllBlocks.FlexiSplitGraphTrack)
		visual { SimpleBlockEntityVisualizer.Factory(::FlexiTrackVisual) }
		renderer { NonNullFunction(::FlexiTrackRenderer) }
	}
}
