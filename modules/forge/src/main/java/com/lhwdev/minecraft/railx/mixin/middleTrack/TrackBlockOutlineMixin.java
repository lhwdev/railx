package com.lhwdev.minecraft.railx.mixin.middleTrack;

import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackBlockOutline.class)
public class TrackBlockOutlineMixin {
	@Inject(method = "pickCurves", at = @At("RETURN"))
	private static void pickUnloadedCurves(CallbackInfo ci) {
		if(TrackBlockOutline.result != null) return;
		if(Minecraft.getInstance().level == null) return;
		MiddleTrackOutline.INSTANCE.pickCurves();
	}
}
