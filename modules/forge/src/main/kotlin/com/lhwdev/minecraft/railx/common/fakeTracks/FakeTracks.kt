package com.lhwdev.minecraft.railx.common.fakeTracks

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.trains.track.BezierConnection
import net.minecraft.world.level.Level


object FakeTracks {
	fun createFakeTrackState(curve: BezierConnection): FakeTrackState {
		val middleTracks = RailXConfig.Server.middleTrack.enabled.get()
		val fakeTracks = !RailXConfig.Server.common.noFakeTracks.get()
		
		return if(fakeTracks) {
			FakeTrackStateImpl(curve)
		} else {
			if(middleTracks) {
				MiddleOnlyFakeTrackState(curve)
			} else {
				FakeTrackState.Empty
			}
		}
	}
	
	
	fun manageFakeTracksFor(level: Level, curve: BezierConnection, remove: Boolean) {
		val state = curve.fakeTrackState
		if(RailXConfig.Server.common.noFakeTracksForUnloadedChunk.get() && !state.isAllChunksLoaded(level))
			return
		
		state.placeAllFakeTracks(level, remove)
	}
}
