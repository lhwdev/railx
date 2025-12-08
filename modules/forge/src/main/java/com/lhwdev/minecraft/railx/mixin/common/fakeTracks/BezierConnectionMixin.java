package com.lhwdev.minecraft.railx.mixin.common.fakeTracks;

import com.lhwdev.minecraft.railx.common.fakeTracks.FakeTrackState;
import com.lhwdev.minecraft.railx.common.fakeTracks.FakeTracks;
import com.lhwdev.minecraft.railx.common.fakeTracks.IFakeTrackContainer;
import com.simibubi.create.content.trains.track.BezierConnection;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(BezierConnection.class)
public class BezierConnectionMixin implements IFakeTrackContainer {
	@Unique
	private FakeTrackState railx$fakeTrackState;
	
	
	@Override
	public @NotNull FakeTrackState getRalix$fakeTrackState() {
		FakeTrackState state = railx$fakeTrackState;
		if(state == null || state.isInvalid()) {
			state = FakeTracks.INSTANCE.createFakeTrackState((BezierConnection) (Object) this);
			railx$fakeTrackState = state;
		}
		return state;
	}
}
