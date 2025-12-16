package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ServerboundPacketPayload
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand


class FlexiblePlacementPacket(val mainHand: Boolean, val flexible: Boolean) : ServerboundPacketPayload {
	companion object : RailXPacketType<FlexiblePlacementPacket>() {
		override val streamCodec = StreamCodec.composite(
			ByteBufCodecs.BOOL, FlexiblePlacementPacket::mainHand,
			ByteBufCodecs.BOOL, FlexiblePlacementPacket::flexible,
			::FlexiblePlacementPacket,
		)
	}
	
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		val hand = if(mainHand) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
		val stack = player.getItemInHand(hand)
		if(!com.simibubi.create.AllTags.AllBlockTags.TRACKS.matches(stack)) return
		stack.set(AllDataComponents.FlexiblePlacement, flexible)
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.FlexiblePlacement
}
