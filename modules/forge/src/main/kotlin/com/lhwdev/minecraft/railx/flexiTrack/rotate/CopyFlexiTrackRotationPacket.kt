package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.simibubi.create.content.equipment.clipboard.ClipboardContent
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ServerboundPacketPayload
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import com.simibubi.create.AllDataComponents as CreateDataComponents

class CopyFlexiTrackRotationPacket(val direction: FlexiDirection) : ServerboundPacketPayload {
	companion object : RailXPacketType<CopyFlexiTrackRotationPacket>() {
		override val streamCodec = StreamCodec.composite(
			FlexiDirection.STREAM_CODEC, CopyFlexiTrackRotationPacket::direction,
			::CopyFlexiTrackRotationPacket
		)
	}
	
	
	override fun handle(player: ServerPlayer?) {
		if(player == null) return
		
		val heldItem = player.mainHandItem
		
		var clipboardContent = heldItem.getOrDefault(CreateDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY)
		val tag = clipboardContent.copiedValues.orElseGet { CompoundTag() }
		
		val entryTag = CompoundTag { FlexiTrackRotateScrollBehaviors.writeToClipboard(axis = direction, tag = it) }
		tag.put(FlexiTrackRotateScrollBehaviors.ClipboardKey, entryTag)
		
		clipboardContent = clipboardContent.setType(ClipboardOverrides.ClipboardType.WRITTEN)
			.setCopiedValues(tag)
		
		heldItem[CreateDataComponents.CLIPBOARD_CONTENT] = clipboardContent
		
		player.displayClientMessage(
			CreateLang.translate(
				"clipboard.copied_from",
				Component.literal("Curve Rotation").withStyle(ChatFormatting.WHITE)
			).style(ChatFormatting.GREEN).component(), true
		)
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.CopyFlexiTrackRotation
}
