package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.PreciseTrackPlacementOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(TrackPlacementOverlay.class)
public class TrackPlacementOverlayMixin {
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;" +
		"drawString" +
		"(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
	int drawString(
		GuiGraphics instance,
		Font font,
		Component text,
		int x,
		int y,
		int color,
		boolean dropShadow,
		Operation<Integer> original
	) {
		Minecraft mc = Minecraft.getInstance();
		int lineCount = PreciseTrackPlacementOverlay.INSTANCE.getInfoLineCount();
		
		Component message = com.lhwdev.minecraft.railx.common.TrackPlacementOverlay.INSTANCE.getOverlayMessage();
		
		return original.call(
			instance,
			font,
			message,
			mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(message),
			y + 9 * lineCount,
			color,
			dropShadow
		);
	}
}
