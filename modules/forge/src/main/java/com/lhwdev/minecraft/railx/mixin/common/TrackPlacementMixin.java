package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.CreateTrackPlacement;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.TrackPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackPlacement.class)
public class TrackPlacementMixin {
	@Inject(method = "clientTick", at = @At("HEAD"), remap = false)
	private static void beforeClientTick(CallbackInfo ci) {
		CreateTrackPlacement.INSTANCE.setLastOverlay(null);
	}
	
	@ModifyExpressionValue(method = "clientTick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/track/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;" +
		"Lnet/minecraft/world/entity/player/Player;" +
		"Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;" +
		"Lnet/minecraft/world/item/ItemStack;ZZ)" +
		"Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;", ordinal = 0), require = 0)
	private static TrackPlacement.PlacementInfo mapPlacementInfo(TrackPlacement.PlacementInfo original) {
		CreateTrackPlacement.INSTANCE.setLastOverlay(original);
		return original;
	}
}
