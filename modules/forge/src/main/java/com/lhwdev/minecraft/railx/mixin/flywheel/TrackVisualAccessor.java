package com.lhwdev.minecraft.railx.mixin.flywheel;

import com.lhwdev.minecraft.railx.flywheel.TrackVisualExtension;
import com.lhwdev.minecraft.railx.flywheel.TrackVisual_BezierTrackVisualExtension;
import com.simibubi.create.content.trains.track.TrackVisual;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;


@Mixin(TrackVisual.class)
public interface TrackVisualAccessor extends TrackVisualExtension {
	@Override
	@Accessor("visuals")
	@NotNull List<TrackVisual_BezierTrackVisualExtension> getRailx$visuals();
}
