package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.buildTrack.TrackPlan
import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlacementInfo
import com.mojang.serialization.Codec
import net.minecraft.network.codec.ByteBufCodecs


object AllDataComponents {
	val Registry = RailXRegistry
	
	val TrackConnectingFrom = Registry.dataComponentType<FlexiPlacementInfo.TrackPoint>("track_connecting_from") {
		persistent(FlexiPlacementInfo.TrackPoint.CODEC)
		networkSynchronized(FlexiPlacementInfo.TrackPoint.STREAM_CODEC)
	}
	
	val TrackMaxRadius = Registry.dataComponentType<Double>("track_max_radius") {
		persistent(Codec.DOUBLE)
		networkSynchronized(ByteBufCodecs.DOUBLE)
	}
	
	val TrackBuildPlan = Registry.dataComponentType<TrackPlan>("track_plan") {
		persistent(TrackPlan.CODEC)
		// networkSynchronized(TrackPlan.STREAM_CODEC)
	}
	
	fun register() {}
}
