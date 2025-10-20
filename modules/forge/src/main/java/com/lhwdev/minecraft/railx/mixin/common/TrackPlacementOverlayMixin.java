package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.PreciseTrackPlacementOverlay;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(TrackPlacementOverlay.class)
public class TrackPlacementOverlayMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;" +
		"drawString" +
		"(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
	int drawString(GuiGraphics instance, Font font, Component text, int x, int y, int color, boolean dropShadow) {
		Minecraft mc = Minecraft.getInstance();
		boolean active = mc.options.keySprint.isDown();
		int lineCount = PreciseTrackPlacementOverlay.INSTANCE.getInfoLineCount();
		return instance.drawString(
			font,
			CreateLang.translateDirect(
				"track.hold_for_smooth_curve", Component.keybind("key.sprint")
					.withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY)
			).withStyle(ChatFormatting.WHITE),
			x,
			y + 9 * lineCount,
			color,
			dropShadow
		);
	}
}
