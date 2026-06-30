package com.lhwdev.minecraft.railx.mixin.common.noTrackOutline;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.utils.ConfigKt;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackBlockOutline.class)
public class TrackBlockOutlineMixin {
	@Inject(method = "pickCurves", at = @At("HEAD"), cancellable = true, remap = false)
	private static void beforePickCurves(CallbackInfo ci) {
		if(ConfigKt.getOrFalse(RailXConfig.Client.Value.getCommon().getNoTrackOutline())) {
			ci.cancel();
			TrackBlockOutline.result = null;
		}
	}
}
