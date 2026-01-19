package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.common.commands.cleanTrackGraphCommand
import com.lhwdev.minecraft.railx.common.commands.fixBrokenFramedTrainsCommand
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.Commands
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber


@EventBusSubscriber
object AllCommands {
	fun register() {}
	
	@SubscribeEvent
	fun registerCommands(event: RegisterCommandsEvent) {
		val dispatcher = event.dispatcher
		
		with(RailXCommandBuildContext(context = event.buildContext)) {
			val railx = Commands.literal("railx")
				.then(cleanTrackGraphCommand())
				.then(fixBrokenFramedTrainsCommand())
			dispatcher.register(railx)
		}
	}
}


class RailXCommandBuildContext(val context: CommandBuildContext)
