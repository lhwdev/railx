package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.content.trains.track.TrackBlockOutline
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.InputEvent
import com.simibubi.create.AllItems as CreateItems


@OnlyIn(Dist.CLIENT)
object CurvedFlexiTrackInteraction {
	fun onClickInput(event: InputEvent.InteractionKeyMappingTriggered): Boolean {
		// didn't use TracksOutline, as it requires both chunks to be loaded
		val result = TrackBlockOutline.result ?: return false
		
		val mc = Minecraft.getInstance()
		val level = mc.level ?: return false
		val player = mc.player ?: return false
		
		if(event.isUseItem) {
			// split flexi track block
			if(CreateItems.WRENCH.isIn(player.mainHandItem) && !player.isShiftKeyDown) {
				val track = result.blockEntity as? FlexiTrackBlockEntity ?: return false
				
				// TODO: WIP
				return false
			}
		}
		
		return false
	}
}
