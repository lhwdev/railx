package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.config.RailXConfigServer
import com.lhwdev.minecraft.railx.mixin.mixinInit
import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.KotlinModLoadingContext
import java.util.*


@Mod(RailX.modId)
class RailX {
	init {
		//		val modEventBus = FMLJavaModLoadingContext.get().modEventBus
		val modEventBus = KotlinModLoadingContext.get().getKEventBus()
		
		ModLoadingContext.get()
			.registerConfig(ModConfig.Type.SERVER, RailXConfigServer.spec)
		
		Registries.init(modEventBus)
		
		MinecraftForge.EVENT_BUS.register(this)
		
		mixinInit()
	}
	
	@SubscribeEvent
	fun onServerStarting(event: ServerStartingEvent) {
		logger.info("HELLO from server starting")
	}
	
	
	private var ticks = 0L
	private val tickTasks = PriorityQueue<Task>()
	
	private class Task(val tickAt: Long, val task: () -> Unit) : Comparable<Task> {
		override fun compareTo(other: Task): Int = (tickAt - other.tickAt).toInt()
	}
	
	private fun runAfter(deltaTicks: Int, task: () -> Unit): Task =
		Task(tickAt = ticks + deltaTicks, task = task).also { tickTasks.add(it) }
	
	
	@SubscribeEvent
	fun onTick(event: ServerTickEvent) {
		ticks++
		while(true) {
			val next = tickTasks.peek()
			if(next == null) break
			if(next.tickAt <= ticks) {
				tickTasks.poll().task()
			} else {
				break
			}
		}
	}
	
	private var shutdownTask: Task? = null
	
	@SubscribeEvent
	fun onUserJoin(event: EntityJoinLevelEvent) {
		if(event.level.isClientSide) return
		
		if(event.entity is Player) {
			if(shutdownTask != null) {
				tickTasks -= shutdownTask
			}
		}
	}
	
	@SubscribeEvent
	fun onUserLeave(event: EntityLeaveLevelEvent) {
		if(event.level.isClientSide) return
		
		val entity = event.entity
		if(entity is Player) {
			val server = event.level.server!!
			if(server.playerList.playerCount == 0) {
				logger.info("server is about to shutdown")
				runAfter(deltaTicks = 20 * 60) {
					logger.info("shutdown in progress...")
					Runtime.getRuntime().exec("/usr/bin/sh", arrayOf("-c", "sudo shutdown -h +1"))
					server.stopServer()
				}
			}
		}
	}
	
	
	@EventBusSubscriber(modid = modId, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
	object ClientModEvents {
		@SubscribeEvent
		fun onClientSetup(event: FMLClientSetupEvent) {
			// Some client setup code
			logger.info("HELLO FROM CLIENT SETUP")
			logger.info("MINECRAFT NAME >> {}", Minecraft.getInstance().user.name)
		}
	}
	
	companion object {
		const val modId = "railx"
		
		val logger = LogUtils.getLogger()
	}
}
