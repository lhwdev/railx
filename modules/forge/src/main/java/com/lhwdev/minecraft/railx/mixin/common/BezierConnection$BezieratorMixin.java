package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.utils.ConfigKt;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(targets = "com.simibubi.create.content.trains.track.BezierConnection$Bezierator")
public class BezierConnection$BezieratorMixin {
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Couple;getFirst()" +
		"Ljava/lang/Object;", ordinal = 1))
	Object getFirstAsNormalized(Couple<Vec3> instance) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getCommon().getFixTrackBezierSegment()))
			return instance.getFirst();
		
		return instance.getFirst().normalize();
	}
	
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Couple;getSecond()" +
		"Ljava/lang/Object;", ordinal = 1))
	Object getSecondAsNormalized(Couple<Vec3> instance) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getCommon().getFixTrackBezierSegment()))
			return instance.getFirst();
		
		return instance.getSecond().normalize();
	}
}
