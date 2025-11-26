package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.registry.ServerboundPacketBase
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.railx.utils.putCompound
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import com.simibubi.create.AllBlocks as CreateBlocks
import com.simibubi.create.AllSoundEvents as CreateSoundEvents


class CurvedMiddleTrackSelectionPacket(
	val bezier: MiddleBezierSource,
	val segmentIndex: Int,
	val front: Boolean,
	val itemSlot: Int,
) : ServerboundPacketBase() {
	companion object : RailXPacketType<CurvedMiddleTrackSelectionPacket>() {
		override fun read(buffer: FriendlyByteBuf): CurvedMiddleTrackSelectionPacket = CurvedMiddleTrackSelectionPacket(
			MiddleBezierSource.read(buffer),
			segmentIndex = buffer.readVarInt(),
			front = buffer.readBoolean(),
			itemSlot = buffer.readVarInt(),
		)
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		bezier.write(buffer)
		buffer.writeVarInt(segmentIndex)
		buffer.writeBoolean(front)
		buffer.writeVarInt(itemSlot)
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
			?: Vec3.atBottomCenterOf(bezier.middlePos)
		
		if(player.isShiftKeyDown && stack.tag != null) {
			player.displayClientMessage(CreateLang.translateDirect("track_target.clear"), true)
			stack.tag = null
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
				(RailXConfig.Server.flexiTrak.placementLength.get() + 16).toDouble()
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
		
		stack.tag = CompoundTag { tag ->
			tag.put("SelectedPos", NbtUtils.writeBlockPos(fromPos))
			tag.putBoolean("SelectedDirection", front)
			tag.putCompound("Bezier") { bezier ->
				bezier.putInt("Segment", bezierPointLocation.segment)
				bezier.put("Key", NbtUtils.writeBlockPos(bezierPointLocation.curveTarget))
				bezier.putBoolean("FromStack", true)
			}
		}
		
		player.displayClientMessage(CreateLang.translateDirect("track_target.set"), true)
		CreateSoundEvents.CONTROLLER_CLICK.play(level, null, soundOrigin, 1f, 1f)
	}
}
