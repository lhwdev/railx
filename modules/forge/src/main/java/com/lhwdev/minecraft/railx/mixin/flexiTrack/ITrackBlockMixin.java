package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.simibubi.create.content.trains.track.ITrackBlock;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(ITrackBlock.class)
public interface ITrackBlockMixin {
	//	@Redirect(method = "addToListIfConnected", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
	//		"/trains/graph/TrackNodeLocation$DiscoveredLocation;viaTurn" +
	//		"(Lcom/simibubi/create/content/trains/track/BezierConnection;)" +
	//		"Lcom/simibubi/create/content/trains/graph/TrackNodeLocation$DiscoveredLocation;"))
	//	private static TrackNodeLocation.DiscoveredLocation viaTurn(
	//		TrackNodeLocation.DiscoveredLocation instance,
	//		BezierConnection turn
	//	) {
	//		TrackNodeLocationUtils.roundToOriginal(instance);
	//		return instance.viaTurn(turn);
	//	}
}
