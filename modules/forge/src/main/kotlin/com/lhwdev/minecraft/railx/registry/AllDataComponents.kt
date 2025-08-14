package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackPlacement
import com.mojang.serialization.Codec
import net.minecraft.network.codec.ByteBufCodecs


object AllDataComponents {
	val Registry = RailXRegistry
	
	val TrackConnectingFrom = Registry.dataComponentType<FlexiTrackPlacement.TrackPoint>("track_connecting_from") {
		persistent(FlexiTrackPlacement.TrackPoint.CODEC)
		networkSynchronized(FlexiTrackPlacement.TrackPoint.STREAM_CODEC)
	}
	
	val TrackMaxRadius = Registry.dataComponentType<Double>("track_max_radius") {
		persistent(Codec.DOUBLE)
		networkSynchronized(ByteBufCodecs.DOUBLE)
	}
	
	fun register() {}
}
