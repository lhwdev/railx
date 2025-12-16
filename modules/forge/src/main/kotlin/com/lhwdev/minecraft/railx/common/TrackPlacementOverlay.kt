package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.registry.AllKeys
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackBlockItem
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn


@OnlyIn(Dist.CLIENT)
object TrackPlacementOverlay {
	fun getOverlayMessage(): Component {
		val mc = Minecraft.getInstance()
		return if(RailXConfig.Server.flexiTrak.enabled.get() &&
			AllKeys.FlexiblePlacement.key == mc.options.keySprint.key.value
		) {
			val flexible = AllKeys.FlexiblePlacement.isPressed
			
			if(flexible && isHandTrackNotFlexible()) {
				Component.literal("This track cannot be placed flexibly").withStyle(ChatFormatting.RED)
			} else {
				Component.literal("Hold ")
					.append(
						Component.keybind(AllKeys.FlexiblePlacement.description)
							.withStyle(if(flexible) ChatFormatting.GREEN else ChatFormatting.GRAY)
					)
					.append(" for flexible placement")
					.withStyle(ChatFormatting.WHITE)
			}
		} else {
			val active = mc.options.keySprint.isDown
			CreateLang.translateDirect(
				"track.hold_for_smooth_curve",
				Component.keybind("key.sprint")
					.withStyle(if(active) ChatFormatting.GREEN else ChatFormatting.GRAY)
			).withStyle(ChatFormatting.WHITE)
		}
	}
	
	private fun isHandTrackNotFlexible(): Boolean {
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return false
		val item = player.mainHandItem.item as? TrackBlockItem ?: return false
		val block = item.block as? TrackBlock ?: return false
		return FlexiTrackMaterial.toFlexible(block) == null
	}
}
