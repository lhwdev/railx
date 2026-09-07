package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.lhwdev.minecraft.railx.common.carriageTilt.CarriageBogeyWithTilt;
import com.lhwdev.minecraft.railx.flexiTrack.rotate.RotationUtils;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CarriageBogey.class)
public abstract class CarriageBogeyMixin implements CarriageBogeyWithTilt {
	@Shadow
	public abstract TravellingPoint leading();
	
	@Shadow
	public abstract TravellingPoint trailing();
	
	@Shadow public Carriage carriage;
	
	@Unique LerpedFloat railx$roll;
	
	
	@Override
	public @NotNull LerpedFloat getRailx$tilt() {
		return railx$roll;
	}
	
	
	@Inject(method = "<init>(Lcom/simibubi/create/content/trains/bogey/AbstractBogeyBlock;" +
		"ZLnet/minecraft/nbt/CompoundTag;Lcom/simibubi/create/content/trains/entity/TravellingPoint;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint;)V", at = @At("RETURN"))
	void onInitialize(CallbackInfo ci) {
		railx$roll = LerpedFloat.angular();
	}
	
	@Inject(method = "updateAngles", at = @At("RETURN"))
	void updateAngles(CarriageContraptionEntity entity, double distanceMoved, CallbackInfo ci) {
		float zRot = 0;
		
		TravellingPoint leading = leading();
		if(leading.edge == null || carriage.train.derailed) {
			// derailed
		} else {
			TrackGraph graph = carriage.train.graph;
			double cant1 = RotationUtils.getDirectionCant(leading, graph);
			double cant2 = RotationUtils.getDirectionCant(trailing(), graph);
			zRot = AngleHelper.deg((cant1 + cant2) / 2);
		}
		
		railx$roll.setValue(zRot);
	}
}
