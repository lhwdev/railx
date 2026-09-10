package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.carriageTilt.DimensionalCarriageEntityWithTilt;
import com.lhwdev.minecraft.railx.common.carriageTilt.OrientedContraptionEntityWithRoll;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;


@SuppressWarnings("DataFlowIssue")
@Mixin(Carriage.DimensionalCarriageEntity.class)
public class Carriage$DimensionalCarriageEntityMixin implements DimensionalCarriageEntityWithTilt {
	@Unique
	private Couple<@Nullable Double> railx$tilt;
	
	@Override
	public @NotNull Couple<@Nullable Double> getRailx$tilt() {
		return railx$tilt;
	}
	
	
	@Inject(method = "<init>", at = @At("RETURN"))
	void onInitialize(Carriage this$0, CallbackInfo ci) {
		railx$tilt = Couple.create(null, null);
	}
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
		if(RailXConfig.Server.Value.getCommon().getCarriageTilt().isFalse()) return;
		CompoundTag tag = cir.getReturnValue();
		
		if(railx$tilt.both(Objects::nonNull)) {
			var tiltTag = new ListTag();
			tiltTag.add(DoubleTag.valueOf(railx$tilt.getFirst()));
			tiltTag.add(DoubleTag.valueOf(railx$tilt.getSecond()));
			tag.put("railx:Tilt", tiltTag);
		}
	}
	
	@Inject(method = "read", at = @At("RETURN"))
	void onRead(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getCarriageTilt().isFalse()) return;
		
		if(tag.contains("railx:Tilt")) {
			var tiltTag = tag.getList("railx:Tilt", DoubleTag.TAG_DOUBLE);
			railx$tilt.setFirst(tiltTag.getDouble(0));
			railx$tilt.setSecond(tiltTag.getDouble(1));
		}
	}
	
	
	@Inject(method = "alignEntity", at = @At("HEAD"))
	void beforeAlignEntity(CarriageContraptionEntity entity, CallbackInfo ci) {
		var entityWithRoll = (OrientedContraptionEntityWithRoll) entity;
		entityWithRoll.setRailx$prevRoll(entityWithRoll.getRailx$roll());
		
		// TODO: handle leading tilt != trailing tilt case
		float roll;
		if(RailXConfig.Server.Value.getCommon().getCarriageTilt().isFalse()) {
			roll = 0f;
		} else if(railx$tilt.both(Objects::nonNull)) {
			roll = (float) (railx$tilt.getFirst() + railx$tilt.getSecond()) / 2;
		} else {
			roll = 0f;
		}
		entityWithRoll.setRailx$roll(roll);
		
		if(entity.firstPositionUpdate) {
			entityWithRoll.setRailx$prevRoll(roll);
		}
	}
}
