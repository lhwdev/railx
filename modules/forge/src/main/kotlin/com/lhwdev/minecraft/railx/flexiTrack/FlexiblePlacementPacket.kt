package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.registry.ServerboundPacketBase
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand


class FlexiblePlacementPacket(val mainHand: Boolean, val flexible: Boolean) : ServerboundPacketBase() {
	companion object : RailXPacketType<FlexiblePlacementPacket>() {
		override fun read(buffer: FriendlyByteBuf): FlexiblePlacementPacket = FlexiblePlacementPacket(
			mainHand = buffer.readBoolean(),
			flexible = buffer.readBoolean(),
		)
	}
	
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		val hand = if(mainHand) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
		val stack = player.getItemInHand(hand)
		if(!com.simibubi.create.AllTags.AllItemTags.TRACKS.matches(stack)) return
		stack.orCreateTag.putBoolean("railx:FlexiblePlacement", flexible)
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeBoolean(mainHand)
		buffer.writeBoolean(flexible)
	}
}
