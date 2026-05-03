package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.InputEvent
import com.simibubi.create.AllTags as CreateTags


@OnlyIn(Dist.CLIENT)
object CurvedMiddleTrackInteraction {
	fun onClickInput(event: InputEvent.InteractionKeyMappingTriggered): Boolean {
		val selection = MiddleTrackOutline.result ?: return false
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return false
		
		if(event.isUseItem) {
			val heldItem = player.mainHandItem
			val item = heldItem.item
			
			if(CreateTags.AllItemTags.TRACKS.matches(heldItem)) {
				player.displayClientMessage(
					CreateLang.translateDirect("track.turn_start").withStyle(ChatFormatting.RED),
					true
				)
				player.swing(InteractionHand.MAIN_HAND)
				return true
			}
			
			if(item is TrackTargetingBlockItem && useTrackTargetingOnCurve(selection)) {
				player.swing(InteractionHand.MAIN_HAND)
				return true
			}
			
			// cannot break tracks
			// if(CreateItems.WRENCH.isIn(heldItem) && player.isShiftKeyDown())
		}
		
		if(event.isAttack) {
			// do nothing; cannot break tracks
			return true
		}
		return false
	}
	
	private fun useTrackTargetingOnCurve(selection: MiddleBezierPointSelection): Boolean {
		val mc = Minecraft.getInstance()
		val player = mc.player!!
		val level = mc.level!!
		val front = player.lookAngle.dot(selection.tangent) < 0
		
		val connection = GlobalConnections[level][selection.curve.bePositions]
		if(connection == null) {
			player.displayClientMessage(
				CreateLang.translateDirect("track_target.invalid").withStyle(ChatFormatting.RED),
				true
			)
			return true
		}
		val packet = CurvedMiddleTrackSelectionPacket(
			bezier = selection.bezierSource,
			segmentIndex = selection.segmentIndex,
			front = front,
			itemSlot = player.inventory.selected,
		)
		AllPackets.sendToServer(packet)
		return true
	}
}
