package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.splitGraph.SplittingTrackNodeSync
import com.lhwdev.minecraft.railx.splitGraph.TrackGraphConnectedId
import com.lhwdev.minecraft.railx.throttle.ThrottlesServer
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber


@EventBusSubscriber
object ServerEvents {
	@SubscribeEvent
	fun onTick(@Suppress("unused") event: TickEvent.ServerTickEvent) {
		if(event.phase != TickEvent.Phase.END) return
		TrackGraphConnectedId.onTick()
		SplittingTrackNodeSync.onTick()
	}
	
	@SubscribeEvent
	fun onServerLevelTick(event: TickEvent.LevelTickEvent) {
		if(event.phase != TickEvent.Phase.END) return
		val level = event.level
		if(level.isClientSide) return
		
		ThrottlesServer.tick(level)
	}
}
