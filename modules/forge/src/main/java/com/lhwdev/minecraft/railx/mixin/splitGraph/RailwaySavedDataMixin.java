package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.RailwayServerGlobals;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.RailwaySavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(RailwaySavedData.class)
public class RailwaySavedDataMixin {
	@ModifyExpressionValue(method = "load(Lnet/minecraft/nbt/CompoundTag;)" +
		"Lcom/simibubi/create/content/trains/RailwaySavedData;", at = @At(value = "NEW", target = "()Lcom/simibubi" +
		"/create/content/trains/RailwaySavedData;", remap = false), remap = false)
	private static RailwaySavedData beforeLoad(RailwaySavedData sd) {
		RailwayServerGlobals.INSTANCE.setSavedData(sd);
		return sd;
	}
	
	@Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)Lcom/simibubi/create/content/trains/RailwaySavedData;",
		at = @At("RETURN"), remap = false)
	private static void afterLoad(CallbackInfoReturnable<RailwaySavedData> cir) {
		RailwayServerGlobals.INSTANCE.setSavedData(null);
	}
}
