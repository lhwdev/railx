package com.lhwdev.minecraft.railx.mixin.middleTrack;


import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Inject(method = "compileChunks", at = @At("RETURN"))
	void afterCompileSections(Camera camera, CallbackInfo ci) {
		MiddleTrackClient.INSTANCE.tick();
	}
}
