package com.lhwdev.minecraft.railx.mixin.common.longWaitLookahead;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import net.createmod.catnip.data.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;


@Mixin(value = Navigation.class, priority = 2000)
public class NavigationMixin {
	@Shadow public double distanceToSignal;
	@Shadow public int ticksWaitingForSignal;
	@Shadow public Pair<UUID, Boolean> waitingForSignal;
	
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
	double clampScanDistance(double value, double min, double max, Operation<Double> original) {
		double lookaheadDistance =
			RailXConfig.Server.Value.getCommon().getAwaitDepartureForSignalDistance().getAsDouble();
		if(lookaheadDistance == 0.0) lookaheadDistance = min;
		
		return original.call(value, lookaheadDistance, max);
	}
	
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity" +
		"/Train;leaveStation()V"), require = 0)
	void beforeLeaveStation(Train instance, Operation<Void> original) {
		double lookaheadDistance =
			RailXConfig.Server.Value.getCommon().getAwaitDepartureForSignalDistance().getAsDouble();
		if(lookaheadDistance == 0.0) {
			original.call(instance);
			return;
		}
		
		if(this.waitingForSignal != null && this.distanceToSignal < lookaheadDistance) {
			this.ticksWaitingForSignal++;
		} else {
			original.call(instance);
		}
	}
}
