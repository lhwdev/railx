package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.buildTrack.TrackPlan
import com.mojang.serialization.Codec
import net.minecraft.network.codec.ByteBufCodecs


object AllDataComponents {
	val Registry = RailXRegistry
	
	val FlexiblePlacement = Registry.dataComponentType<Boolean>("flexible_placement") {
		persistent(Codec.BOOL)
		networkSynchronized(ByteBufCodecs.BOOL)
	}
	
	val TrackBuildPlan = Registry.dataComponentType<TrackPlan>("track_plan") {
		persistent(TrackPlan.CODEC)
		// networkSynchronized(TrackPlan.STREAM_CODEC)
	}
	
	fun register() {}
}
