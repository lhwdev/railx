package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.throttle.ThrottlesClient;
import com.lhwdev.minecraft.railx.utils.ConfigKt;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ControlsHandler.class)
public class ControlsHandlerMixin {
	@Inject(method = "levelUnloaded", at = @At("RETURN"))
	private static void onLevelUnloaded(LevelAccessor level, CallbackInfo ci) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getThrottle().getEnabled())) return;
		ThrottlesClient.INSTANCE.levelUnloaded(level);
	}
	
	@Inject(method = "startControlling", at = @At("RETURN"))
	private static void onStartControlling(CallbackInfo ci) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getThrottle().getEnabled())) return;
		ThrottlesClient.INSTANCE.startControlling();
	}
	
	@Inject(method = "stopControlling", at = @At("RETURN"))
	private static void onStopControlling(CallbackInfo ci) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getThrottle().getEnabled())) return;
		ThrottlesClient.INSTANCE.stopControlling();
	}
	
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private static void tick(CallbackInfo ci) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getThrottle().getEnabled())) return;
		ThrottlesClient.INSTANCE.tick();
		ci.cancel();
	}
}
