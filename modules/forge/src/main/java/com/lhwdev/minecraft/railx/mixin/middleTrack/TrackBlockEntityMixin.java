package com.lhwdev.minecraft.railx.mixin.middleTrack;

import com.lhwdev.minecraft.railx.common.FakeTracks;
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddlesKt;
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackLikeBlockEntity;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin extends SmartBlockEntity implements MiddleTrackLikeBlockEntity {
	@Unique
	private List<@NotNull BezierConnection> railx$previousConnections;
	
	public TrackBlockEntityMixin(
		BlockEntityType<?> type,
		BlockPos pos,
		BlockState blockState
	) {
		super(type, pos, blockState);
	}
	
	@Inject(method = "<init>", at = @At("RETURN"))
	void onInitialize(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
		railx$previousConnections = getConnectionValues();
	}
	
	@Override
	public @NotNull List<@NotNull BezierConnection> getConnectionValues() {
		return List.copyOf(connections.values());
	}
	
	@Inject(method = "read", at = @At("RETURN"))
	void afterRead(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
		Level level = this.level;
		if(level == null) return;
		if(level.isClientSide) ConnectionMiddlesKt.getGlobalConnections().get(level)
			.updateTrack((TrackBlockEntity) (Object) this, railx$previousConnections);
		railx$previousConnections = getConnectionValues();
	}
	
	@Unique
	@Override
	public void onLoad() {
		super.onLoad();
		Level level = Objects.requireNonNull(this.level);
		if(level.isClientSide) ConnectionMiddlesKt.getGlobalConnections().get(level)
			.updateTrack((TrackBlockEntity) (Object) this, railx$previousConnections);
		railx$previousConnections = getConnectionValues();
	}
	
	@Unique
	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		removeFromCurveInteraction();
		
		Level level = Objects.requireNonNull(this.level);
		if(level.isClientSide) ConnectionMiddlesKt.getGlobalConnections().get(level)
			.removeTrack((TrackBlockEntity) (Object) this, railx$previousConnections);
		railx$previousConnections = List.of();
	}
	
	@Inject(method = "remove", at = @At("HEAD"))
	void onRemove(CallbackInfo ci) {
		Level level = Objects.requireNonNull(this.level);
		if(level.isClientSide) ConnectionMiddlesKt.getGlobalConnections().get(level)
			.removeTrack((TrackBlockEntity) (Object) this, railx$previousConnections);
		railx$previousConnections = List.of();
	}
	
	@Shadow
	private void removeFromCurveInteraction() {}
	
	@Shadow
	Map<BlockPos, BezierConnection> connections;
	
	/**
	 * @author lhwdev
	 * @reason too lazy to inject
	 */
	@Overwrite
	public void manageFakeTracksAlong(BezierConnection bc, boolean remove) {
		FakeTracks.manageFakeTracksAlong((TrackBlockEntity) (Object) this, bc, remove);
	}
}
