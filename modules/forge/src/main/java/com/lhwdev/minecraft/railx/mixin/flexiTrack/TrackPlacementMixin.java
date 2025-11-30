package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackPlacementClient;
import com.simibubi.create.content.trains.track.TrackPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackPlacement.class)
public class TrackPlacementMixin {
	@Inject(method = "clientTick", at = @At("HEAD"), cancellable = true, remap = false)
	private static void onCancelClientTick(CallbackInfo ci) {
		FlexiTrackPlacementClient.INSTANCE.clientTick(ci);
	}
}
