package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.throttle.Throttles;
import com.lhwdev.minecraft.railx.throttle.TrainWithThrottle;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;


@Mixin(Train.class)
public class TrainMixin implements TrainWithThrottle {
	@Unique
	private Throttles.Throttle railx$throttle;
	
	
	@Override
	@NotNull
	public Throttles.Throttle getRailx$throttle() {
		var previous = railx$throttle;
		if(previous == null) return Throttles.Throttle.Neutral;
		return previous;
	}
	
	@Override
	public void setRailx$throttle(@NotNull Throttles.Throttle throttle) {
		railx$throttle = throttle;
	}
	
	
	@Inject(method = "write", at = @At("RETURN"), remap = false)
	void onWrite(CallbackInfoReturnable<CompoundTag> cir) {
		CompoundTag tag = cir.getReturnValue();
		if(RailXConfig.Server.Value.getThrottle().getEnabled().get())
			tag.put("railx:Throttle", getRailx$throttle().write());
	}
	
	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void onRead(
		CompoundTag tag,
		Map<UUID, TrackGraph> trackNetworks,
		DimensionPalette dimensions,
		CallbackInfoReturnable<Train> cir
	) {
		var train = cir.getReturnValue();
		var throttleTag = tag.get("railx:Throttle");
		if(throttleTag != null)
			((TrainMixin) (Object) train).railx$throttle = Throttles.Throttle.Companion.read(throttleTag);
	}
}
