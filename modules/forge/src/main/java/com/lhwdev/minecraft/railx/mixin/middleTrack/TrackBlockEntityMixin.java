package com.lhwdev.minecraft.railx.mixin.middleTrack;

import com.lhwdev.minecraft.railx.middleTrack.FakeTracks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin extends SmartBlockEntity {
	public TrackBlockEntityMixin(
		BlockEntityType<?> type,
		BlockPos pos,
		BlockState blockState
	) {
		super(type, pos, blockState);
	}
	
	//	@Inject(method = "initialize", at = @At("RETURN"))
	//	public void onInitialize(CallbackInfo ci) {
	//		MiddleTracks.INSTANCE.onTrackBlockEntityUpdate((TrackBlockEntity) (Object) this, false);
	//	}
	
	/**
	 * @author lhwdev
	 * @reason too lazy to inject
	 */
	@Overwrite
	public void manageFakeTracksAlong(BezierConnection bc, boolean remove) {
		FakeTracks.manageFakeTracksAlong((TrackBlockEntity) (Object) this, bc, remove);
	}
	
	//	@Inject(method = "invalidate", at = @At("RETURN"))
	//	void onRemoved(CallbackInfo ci) {
	//		MiddleTracks.INSTANCE.onTrackBlockEntityUpdate((TrackBlockEntity) (Object) this, true);
	//	}
}
