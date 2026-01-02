package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.BezierConnectionUtils;
import com.lhwdev.minecraft.railx.common.IBezierConnectionExtension;
import com.simibubi.create.content.trains.track.BezierConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(BezierConnection.class)
public class BezierConnectionMixin implements IBezierConnectionExtension {
	@Unique
	private double railx$minRadius = -1;
	
	@Override
	public double minRadius() {
		if(railx$minRadius == -1) {
			railx$minRadius = BezierConnectionUtils.calculateMinRadius((BezierConnection) (Object) this);
		}
		return railx$minRadius;
	}
	
	@Override
	public void onCurveUpdated() {
		railx$minRadius = -1;
	}
}
