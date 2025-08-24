package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem;
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackPlacement;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackPlacement.class)
public class TrackPlacementMixin {
	@Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
	private static void onCancelClientTick(CallbackInfo ci) {
		FlexiTrackPlacement.INSTANCE.clientTick(ci);
	}
}
