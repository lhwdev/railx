package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.common.commands.cleanTrackGraphCommand
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.Commands
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.neoforge.event.RegisterCommandsEvent


@EventBusSubscriber
object AllCommands {
	fun register() {}
	
	@SubscribeEvent
	private fun registerCommands(event: RegisterCommandsEvent) {
		val dispatcher = event.dispatcher
		
		with(RailXCommandBuildContext(context = event.buildContext)) {
			val railx = Commands.literal("railx")
				.then(cleanTrackGraphCommand())
			dispatcher.register(railx)
		}
	}
}


class RailXCommandBuildContext(val context: CommandBuildContext)
