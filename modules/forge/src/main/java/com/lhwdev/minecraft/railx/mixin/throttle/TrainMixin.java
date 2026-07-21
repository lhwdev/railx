package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.throttle.Throttles;
import com.lhwdev.minecraft.railx.throttle.ThrottlesServer;
import com.lhwdev.minecraft.railx.throttle.TrainWithThrottle;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;


@Mixin(Train.class)
public class TrainMixin implements TrainWithThrottle {
	@Unique
	private Throttles.Throttle railx$throttle;
	
	
	@Override
	public Throttles.@NotNull Throttle getRailx$throttle() {
		var previous = railx$throttle;
		if(previous == null) return Throttles.Throttle.NeutralStop;
		return previous;
	}
	
	@Override
	public void setRailx$throttle(Throttles.@NotNull Throttle throttle) {
		railx$throttle = throttle;
	}
	
	
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Train;" +
		"tickPassiveSlowdown()V"))
	void tickUpdateSpeed(Level level, CallbackInfo ci) {
		if(level.isClientSide) return;
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isFalse()) return;
		
		ThrottlesServer.INSTANCE.tickTrain((Train) (Object) this);
	}
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(CallbackInfoReturnable<CompoundTag> cir) {
		CompoundTag tag = cir.getReturnValue();
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isTrue())
			tag.put("railx:Throttle", getRailx$throttle().write());
	}
	
	@Inject(method = "read", at = @At("RETURN"))
	private static void onRead(
		CompoundTag tag,
		HolderLookup.Provider registries,
		Map<UUID, TrackGraph> trackNetworks,
		DimensionPalette dimensions,
		CallbackInfoReturnable<Train> cir
	) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isTrue()) {
			var train = cir.getReturnValue();
			var throttleTag = tag.get("railx:Throttle");
			if(throttleTag != null) {
				((TrainMixin) (Object) train).railx$throttle = Throttles.Throttle.Companion.read(throttleTag);
			}
		}
	}
}
