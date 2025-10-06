package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.realisticSpeed.RealisticTrainSpeed;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;


@Mixin(value = Train.class, remap = false)
public class TrainMixin {
	@Unique
	private RealisticTrainSpeed railx$realisticSpeed;
	
	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		railx$realisticSpeed = new RealisticTrainSpeed((Train) (Object) this);
	}
	
	
	@Inject(method = "tickPassiveSlowdown", at = @At("HEAD"), cancellable = true)
	void onTickPassiveSlowdown(CallbackInfo ci) {
		boolean handled = railx$realisticSpeed.handleTickSpeed();
		if(handled) ci.cancel();
	}
	
	@Inject(method = "approachTargetSpeed", at = @At("HEAD"), cancellable = true)
	void onApproachTargetSpeed(float accelerationMod, CallbackInfo ci) {
		boolean handled = railx$realisticSpeed.handleApproachTargetSpeed(accelerationMod);
		if(handled) ci.cancel();
	}
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(
		DimensionPalette dimensions,
		HolderLookup.Provider registries,
		CallbackInfoReturnable<CompoundTag> cir
	) {
		CompoundTag tag = cir.getReturnValue();
		CompoundTag realisticSpeedTag = railx$realisticSpeed.write();
		if(realisticSpeedTag != null && !RailXConfig.Server.Value.getRealisticSpeed().getRemovePrevious().getAsBoolean())
			tag.put("railx:RealisticSpeed", realisticSpeedTag);
	}
	
	@Inject(method = "read", at = @At("RETURN"))
	private static void onRead(
		CompoundTag tag,
		HolderLookup.Provider registries,
		Map<UUID, TrackGraph> trackNetworks,
		DimensionPalette dimensions,
		CallbackInfoReturnable<Train> cir
	) {
		RealisticTrainSpeed realisticSpeed = ((TrainMixin) (Object) cir.getReturnValue()).railx$realisticSpeed;
		Tag realisticSpeedTag = tag.get("railx:RealisticSpeed");
		if(realisticSpeedTag instanceof CompoundTag t) realisticSpeed.read(t);
	}
}
