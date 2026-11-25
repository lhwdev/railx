package com.lhwdev.minecraft.railx.mixin.flywheel;


import com.lhwdev.minecraft.railx.flywheel.TrackVisual_BezierTrackVisualExtension;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackVisual;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(targets = "com.simibubi.create.content.trains.track.TrackVisual.BezierTrackVisual")
public class TrackVisual$BezierTrackVisualMixin implements TrackVisual_BezierTrackVisualExtension {
	@Unique
	private BezierConnection railx$bc;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	void onInitialize(TrackVisual this$0, BezierConnection bc, CallbackInfo ci) {
		railx$bc = bc;
	}
	
	
	@Override
	public @NotNull BezierConnection getRailx$bc() {
		return railx$bc;
	}
}
