package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.rotate.CopyFlexiTrackRotationPacket
import com.lhwdev.minecraft.railx.utils.flexiDirection
import com.simibubi.create.content.trains.track.TrackBlockOutline
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.network.PacketDistributor
import com.simibubi.create.AllBlocks as CreateBlocks
import com.simibubi.create.AllItems as CreateItems


@OnlyIn(Dist.CLIENT)
object CurvedFlexiTrackInteraction {
	fun onClickInput(event: InputEvent.InteractionKeyMappingTriggered): Boolean {
		// didn't use TracksOutline, as it requires both chunks to be loaded
		val result = TrackBlockOutline.result ?: return false
		
		val mc = Minecraft.getInstance()
		val level = mc.level ?: return false
		val player = mc.player ?: return false
		if(player.isSpectator) return false
		
		val heldItem = player.mainHandItem
		if(event.isUseItem) {
			// split flexi track block
			if(CreateItems.WRENCH.isIn(heldItem) && !player.isShiftKeyDown) {
				val track = result.blockEntity as? FlexiTrackBlockEntity ?: return false
				
				// TODO: WIP
				return false
			}
		}
		
		if((event.isAttack || event.isUseItem) && CreateBlocks.CLIPBOARD.isIn(heldItem)) {
			if(RailXConfig.Client.flexiTrak.rightClickCurveToCopy.isFalse) return false
			if(player.isShiftKeyDown) return false
			
			val direction = result.flexiDirection
			PacketDistributor.sendToServer(CopyFlexiTrackRotationPacket(direction))
			return true
		}
		
		return false
	}
}
