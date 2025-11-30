package com.lhwdev.minecraft.railx.middleTrack;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;


// one profound aged Kotlin issue: cannot override getProperty by property

public interface MiddleTrackLikeBlockEntity {
	@NotNull BlockPos getBlockPos();
	
	@NotNull List<BezierConnection> getConnectionValues();
}
