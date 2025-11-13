package com.lhwdev.minecraft.railx.mixin.common.reservedSignal;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.ReservedAwareSignalBlockEntity;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SignalBlockEntity.class)
public abstract class SignalBlockEntityMixin extends SmartBlockEntity implements ReservedAwareSignalBlockEntity {
	public SignalBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}
	
	@Shadow
	public abstract @Nullable SignalBoundary getSignal();
	
	
	@Unique
	private boolean railx$isReserved;
	
	@Override
	public boolean getRailx$isReservedBySelf() {
		return railx$isReserved;
	}
	
	
	@Inject(method = "tick", at = @At("TAIL"))
	void tickServer(CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getReservedSignal().isFalse()) return;
		SignalBoundary boundary = getSignal();
		if(boundary == null) return; // won't happen but...
		var side = boundary.blockEntities.getFirst().containsKey(getBlockPos());
		var group = Create.RAILWAYS.signalEdgeGroups.get(boundary.groups.get(side));
		if(group == null) return;
		var reserved = group.reserved == boundary;
		if(railx$isReserved != reserved) {
			railx$isReserved = reserved;
			sendData(); // client only
		}
	}
	
	
	@Inject(method = "write", at = @At("RETURN"))
	void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getReservedSignal().isFalse()) return;
		if(clientPacket) tag.putBoolean("railx:Reserved", railx$isReserved);
	}
	
	@Inject(method = "read", at = @At("RETURN"))
	void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getReservedSignal().isFalse()) return;
		if(clientPacket) railx$isReserved = tag.getBoolean("railx:Reserved");
	}
}
