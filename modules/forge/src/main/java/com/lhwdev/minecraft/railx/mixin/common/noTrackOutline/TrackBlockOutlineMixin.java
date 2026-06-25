package com.lhwdev.minecraft.railx.mixin.common.noTrackOutline;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackBlockOutline.class)
public class TrackBlockOutlineMixin {
	@Inject(method = "pickCurves", at = @At("HEAD"), cancellable = true)
	private static void beforePickCurves(CallbackInfo ci) {
		if(RailXConfig.Client.Value.getCommon().getNoTrackOutline().isTrue()) {
			ci.cancel();
			TrackBlockOutline.result = null;
		}
	}
}
