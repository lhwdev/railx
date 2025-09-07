package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = TrackBlockOutline.class, remap = false)
public class TrackBlockOutlineMixin {
	@Inject(method = "drawCustomBlockSelection", at = @At("HEAD"), cancellable = true)
	private static void onDrawCustomBlockSelection(RenderHighlightEvent.Block event, CallbackInfo ci) {
		if(FlexiTrackBlockOutline.INSTANCE.drawCustomBlockSelection(event)) {
			ci.cancel();
		}
	}
}
