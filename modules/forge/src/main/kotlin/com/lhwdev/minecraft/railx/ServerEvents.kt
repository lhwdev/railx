package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.realisticSpeed.control.RealisticSpeedControl
import com.lhwdev.minecraft.railx.splitGraph.SplittingTrackNodeSync
import com.lhwdev.minecraft.railx.splitGraph.TrackGraphConnectedId
import com.lhwdev.minecraft.railx.throttle.ThrottlesServer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent


@EventBusSubscriber
object ServerEvents {
	@SubscribeEvent
	fun onTick(@Suppress("unused") event: ServerTickEvent.Post) {
		TrackGraphConnectedId.onTick()
		SplittingTrackNodeSync.onTick()
	}
	
	@SubscribeEvent
	fun onServerLevelTick(event: LevelTickEvent.Post) {
		val level = event.level
		if(level.isClientSide) return
		
		ThrottlesServer.tick(level)
		RealisticSpeedControl.tick(level)
	}
}
