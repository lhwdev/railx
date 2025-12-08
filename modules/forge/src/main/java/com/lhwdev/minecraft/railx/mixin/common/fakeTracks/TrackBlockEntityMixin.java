package com.lhwdev.minecraft.railx.mixin.common.fakeTracks;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.fakeTracks.FakeTracks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;


@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin extends SmartBlockEntity {
	public TrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}
	
	@Inject(method = "<init>", at = @At("RETURN"))
	void afterInitialize(CallbackInfo ci) {
		var tickRate = RailXConfig.Server.Value.getCommon().getFakeTracksManageTickRate().getAsInt();
		if(tickRate != 0) setLazyTickRate(tickRate);
	}
	
	@Inject(method = "manageFakeTracksAlong", at = @At("HEAD"), cancellable = true)
	void manageFakeTracksAlong(BezierConnection bc, boolean remove, CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getOptimizeFakeTracks().isFalse()) return;
		
		FakeTracks.INSTANCE.manageFakeTracksFor(Objects.requireNonNull(level), bc, remove);
		ci.cancel();
	}
}
