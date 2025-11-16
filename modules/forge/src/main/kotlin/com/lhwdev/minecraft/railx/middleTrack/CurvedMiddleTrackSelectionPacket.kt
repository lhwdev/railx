package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ServerboundPacketPayload
import net.minecraft.ChatFormatting
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import com.simibubi.create.AllBlocks as CreateBlocks
import com.simibubi.create.AllDataComponents as CreateDataComponents
import com.simibubi.create.AllSoundEvents as CreateSoundEvents


class CurvedMiddleTrackSelectionPacket(
	val bezier: MiddleBezierSource,
	val segmentIndex: Int,
	val front: Boolean,
	val itemSlot: Int,
) : ServerboundPacketPayload {
	companion object : RailXPacketType<CurvedMiddleTrackSelectionPacket>() {
		override val streamCodec = StreamCodec.composite(
			MiddleBezierSource.STREAM_CODEC, CurvedMiddleTrackSelectionPacket::bezier,
			ByteBufCodecs.VAR_INT, CurvedMiddleTrackSelectionPacket::segmentIndex,
			ByteBufCodecs.BOOL, CurvedMiddleTrackSelectionPacket::front,
			ByteBufCodecs.VAR_INT, CurvedMiddleTrackSelectionPacket::itemSlot,
			::CurvedMiddleTrackSelectionPacket,
		)
	}
	
	override fun handle(player: ServerPlayer?) {
		if(!MiddleTrackInteraction.enabled) return
		if(player == null) return
		val level = player.level()
		
		if(player.inventory.selected != itemSlot) return
		val stack = player.inventory.getItem(itemSlot)
		if(stack.item !is TrackTargetingBlockItem) return
		
		val curve = bezier.resolveCurve(level)
		val soundOrigin = curve?.let { it.getPosition(it.getSegmentT(bezier.index).toDouble()) }
			?: bezier.middlePos.bottomCenter
		
		if(player.isShiftKeyDown && stack.has(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)) {
			player.displayClientMessage(CreateLang.translateDirect("track_target.clear"), true)
			stack.remove(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)
			stack.remove(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION)
			stack.remove(CreateDataComponents.TRACK_TARGETING_ITEM_BEZIER)
			CreateSoundEvents.CONTROLLER_CLICK.play(level, null, soundOrigin, 1f, .5f)
			return
		}
		
		if(curve == null) {
			player.displayClientMessage(
				CreateLang.translateDirect("track_target.invalid").withStyle(ChatFormatting.RED),
				true
			)
			CreateSoundEvents.DENY.play(level, null, soundOrigin, .5f, 1f)
			return
		}
		
		val type = if(CreateBlocks.TRACK_SIGNAL.isIn(stack)) EdgePointType.SIGNAL else EdgePointType.STATION
		val fromPos = curve.bePositions.first
		if(
			!fromPos.closerThan(
				player.blockPosition(),
				(RailXConfig.Server.flexiTrak.placementLength.asInt + 16).toDouble()
			)
		) return
		
		val bezierPointLocation = BezierTrackPointLocation(curve.key, segmentIndex)
		val result = MiddleTrackInteraction.withGraphLocation(level, curve, bezierPointLocation, type, front)
			.result
		
		result.feedback?.let { feedback ->
			player.displayClientMessage(
				CreateLang.translateDirect(feedback).withStyle(ChatFormatting.RED),
				true
			)
			CreateSoundEvents.DENY.play(level, null, soundOrigin, .5f, 1f)
			return
		}
		
		stack.set(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS, fromPos)
		stack.set(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION, front)
		stack.set(CreateDataComponents.TRACK_TARGETING_ITEM_BEZIER, bezierPointLocation)
		
		player.displayClientMessage(CreateLang.translateDirect("track_target.set"), true)
		CreateSoundEvents.CONTROLLER_CLICK.play(level, null, soundOrigin, 1f, 1f)
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.CurvedMiddleTrackSelection
}
