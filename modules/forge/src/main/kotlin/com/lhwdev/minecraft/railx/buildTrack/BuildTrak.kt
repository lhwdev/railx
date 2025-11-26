package com.lhwdev.minecraft.railx.buildTrack

import com.lhwdev.minecraft.railx.RailXConfig


object BuildTrak {
	val enabled: Boolean
		get() = RailXConfig.Server.buildTrak.enabled.get()
	
	// TODO: track plan by item?
	val currentPlan: TrackPlan
		get() = DummyTrackPlan
}
