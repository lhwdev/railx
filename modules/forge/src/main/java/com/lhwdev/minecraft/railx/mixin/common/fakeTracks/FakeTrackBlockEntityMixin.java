package com.lhwdev.minecraft.railx.mixin.common.fakeTracks;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;


@Mixin(FakeTrackBlockEntity.class)
public abstract class FakeTrackBlockEntityMixin extends SyncedBlockEntity {
	public FakeTrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	@Override
	public void onLoad() {
		super.onLoad();
		if(RailXConfig.Server.Value.getCommon().getNoFakeTracks().isTrue())
			Objects.requireNonNull(level).removeBlock(worldPosition, false);
	}
	
	@Inject(method = "randomTick", at = @At("HEAD"))
	void randomTick(CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getNoFakeTracks().isTrue())
			Objects.requireNonNull(level).removeBlock(worldPosition, false);
	}
}
