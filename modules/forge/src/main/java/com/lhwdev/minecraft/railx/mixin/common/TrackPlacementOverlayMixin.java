package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.PreciseTrackPlacementOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
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
		boolean active = mc.options.keySprint.isDown();
		int lineCount = PreciseTrackPlacementOverlay.INSTANCE.getInfoLineCount();
		return original.call(
			instance,
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
