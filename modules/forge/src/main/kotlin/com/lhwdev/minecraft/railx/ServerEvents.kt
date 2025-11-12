package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.splitGraph.SplittingTrackNodeSync
import com.lhwdev.minecraft.railx.splitGraph.TrackGraphConnectedId
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.ServerTickEvent


@EventBusSubscriber
object ServerEvents {
	@SubscribeEvent
	fun onTick(@Suppress("unused") event: ServerTickEvent.Post) {
		TrackGraphConnectedId.onTick()
		SplittingTrackNodeSync.onTick()
	}
}
