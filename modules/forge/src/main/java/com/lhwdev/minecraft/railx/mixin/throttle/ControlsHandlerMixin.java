package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.throttle.ThrottlesClient;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ControlsHandler.class)
public class ControlsHandlerMixin {
	@Inject(method = "levelUnloaded", at = @At("HEAD"))
	private static void onLevelUnloaded(LevelAccessor level, CallbackInfo ci) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isFalse()) return;
		ThrottlesClient.INSTANCE.levelUnloaded(level);
	}
	
	@Inject(method = "startControlling", at = @At("HEAD"))
	private static void onStartControlling(CallbackInfo ci) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isFalse()) return;
		ThrottlesClient.INSTANCE.startControlling();
	}
	
	@Inject(method = "stopControlling", at = @At("HEAD"))
	private static void onStopControlling(CallbackInfo ci) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isFalse()) return;
		ThrottlesClient.INSTANCE.stopControlling();
	}
	
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private static void tick(CallbackInfo ci) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isFalse()) return;
		ThrottlesClient.INSTANCE.tick();
		ci.cancel();
	}
}
