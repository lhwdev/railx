package com.lhwdev.minecraft.railx.mixin.flexiTrack.graph;


import com.lhwdev.minecraft.railx.flexiTrack.graph.ITrackNodeLocation;
import com.lhwdev.minecraft.railx.flexiTrack.graph.TrackNodeLocationUtils;
import com.lhwdev.minecraft.railx.utils.VectorKt;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(TrackNodeLocation.class)
public abstract class TrackNodeLocationMixin extends Vec3i implements ITrackNodeLocation {
	public TrackNodeLocationMixin(int x, int y, int z) {super(x, y, z);}
	
	@Shadow
	public abstract Vec3 getLocation();
	
	@Unique
	private Vec3 railx$location;
	
	
	// NOTE: <init>(DDD)V constructor is never called anywhere so far (other than (BlockPos)V one)
	//       so did not handle that case
	@Inject(method = "<init>(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
	void onInit(Vec3 vec, CallbackInfo ci) {
		railx$location = vec;
	}
	
	
	@Inject(method = "getLocation", at = @At("HEAD"), cancellable = true)
	void onGetLocation(CallbackInfoReturnable<Vec3> cir) {
		if(railx$location != null) cir.setReturnValue(railx$location);
	}
	
	
	@Override
	public @Nullable Vec3 getVecLocation() {
		return railx$location;
	}
	
	@Override
	public void setVecLocation(@Nullable Vec3 value) {
		railx$location = value;
	}
	
	@Inject(method = "equalsIgnoreDim", at = @At("RETURN"), cancellable = true)
	void onEqualsIgnoreDim(Object other, CallbackInfoReturnable<Boolean> cir) {
		if(!cir.getReturnValue()) return;
		if(other instanceof TrackNodeLocation node && !VectorKt.closeTo(getLocation(), node.getLocation()))
			cir.setReturnValue(false);
	}
	
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
		Vec3 location = railx$location;
		if(location != null && !TrackNodeLocationUtils.isIntTrackNodeLocation(location)) {
			cir.getReturnValue().put("Vec", VecHelper.writeNBT(location));
		}
	}
	
	@Inject(method = "send", at = @At("RETURN"))
	void onSend(FriendlyByteBuf buffer, DimensionPalette dimensions, CallbackInfo ci) {
		Vec3 location = railx$location;
		if(location == null) location = getLocation();
		buffer.writeVec3(location);
	}
	
	
	// designed to be compatible with vanilla create (send/receive is only used for network packet which does not have
	// to be binary compatible)
	@Inject(method = "read", at = @At("RETURN"))
	private static void onRead(
		CompoundTag tag,
		DimensionPalette dimensions,
		CallbackInfoReturnable<TrackNodeLocation> cir
	) {
		if(tag.get("Vec") instanceof ListTag vec) {
			((TrackNodeLocationMixin) (Object) cir.getReturnValue()).railx$location = VecHelper.readNBT(vec);
		}
	}
	
	@Inject(method = "receive", at = @At("RETURN"))
	private static void onReceive(
		FriendlyByteBuf buffer,
		DimensionPalette dimensions,
		CallbackInfoReturnable<TrackNodeLocation> cir
	) {
		((TrackNodeLocationMixin) (Object) cir.getReturnValue()).railx$location = buffer.readVec3();
	}
}
