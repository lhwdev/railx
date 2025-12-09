package com.lhwdev.minecraft.railx.mixin.flexiTrack.graph;


import com.lhwdev.minecraft.railx.flexiTrack.graph.ITrackNodeLocation;
import com.lhwdev.minecraft.railx.flexiTrack.graph.TrackNodeLocationDelta;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;


@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(TrackNodeLocation.class)
public abstract class TrackNodeLocationMixin extends Vec3i implements ITrackNodeLocation {
	public TrackNodeLocationMixin(int x, int y, int z) {super(x, y, z);}
	
	@Shadow(remap = false)
	public abstract Vec3 getLocation();
	
	@Shadow(remap = false) public int yOffsetPixels;
	
	@Unique
	private TrackNodeLocationDelta railx$location;
	
	
	// NOTE: <init>(DDD)V constructor is never called anywhere so far (other than (BlockPos)V one)
	//       so did not handle that case
	@Inject(method = "<init>(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"), remap = false)
	void onInit(Vec3 vec, CallbackInfo ci) {
		var location = TrackNodeLocationDelta.of((TrackNodeLocation) (Object) this, vec);
		if(location != null) {
			var asYOffset = location.asYOffset();
			if(asYOffset != -1) yOffsetPixels = asYOffset;
			else railx$location = location;
		}
	}
	
	
	@Inject(method = "getLocation", at = @At("HEAD"), cancellable = true, remap = false)
	void onGetLocation(CallbackInfoReturnable<Vec3> cir) {
		if(railx$location != null) cir.setReturnValue(railx$location.getLocation());
	}
	
	
	@Override
	public @Nullable Vec3 getVecLocation() {
		TrackNodeLocationDelta location = railx$location;
		if(location == null) return null;
		return location.getLocation();
	}
	
	@Override
	public void setVecLocation(@Nullable Vec3 value) {
		if(value == null) {
			railx$location = null;
		} else {
			railx$location = TrackNodeLocationDelta.of((TrackNodeLocation) (Object) this, value);
		}
	}
	
	@Inject(method = "equalsIgnoreDim", at = @At("RETURN"), cancellable = true, remap = false)
	void onEqualsIgnoreDim(Object other, CallbackInfoReturnable<Boolean> cir) {
		if(!cir.getReturnValue()) return;
		if(other instanceof TrackNodeLocation node) {
			TrackNodeLocationDelta otherDelta = ((TrackNodeLocationMixin) (Object) node).railx$location;
			cir.setReturnValue(Objects.equals(railx$location, otherDelta));
		}
	}
	
	@Unique
	@Override
	public @NotNull String toString() {
		Vec3 location = getLocation();
		return "TrackNodeLocation(x=" + location.x + ", y=" + location.y + ", z=" + location.z + ")";
	}
	
	
	@Inject(method = "write", at = @At("RETURN"), remap = false)
	void onWrite(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
		TrackNodeLocationDelta location = railx$location;
		if(location != null) {
			cir.getReturnValue().putByteArray("railx:D", location.toByteArray());
		}
	}
	
	@Inject(method = "send", at = @At("RETURN"), remap = false)
	void onSend(FriendlyByteBuf buffer, DimensionPalette dimensions, CallbackInfo ci) {
		TrackNodeLocationDelta location = railx$location;
		if(location == null) {
			buffer.writeBytes(TrackNodeLocationDelta.DefaultAsBytes);
		} else {
			buffer.writeBytes(location.toByteArray());
		}
	}
	
	
	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void onRead(
		CompoundTag tag,
		DimensionPalette dimensions,
		CallbackInfoReturnable<TrackNodeLocation> cir
	) {
		if(tag.get("railx:D") instanceof ByteArrayTag delta) {
			((TrackNodeLocationMixin) (Object) cir.getReturnValue()).railx$location =
				TrackNodeLocationDelta.fromByteArray(cir.getReturnValue(), delta.getAsByteArray());
		}
		
		// compatibility
		if(tag.get("Vec") instanceof ListTag vec) {
			((TrackNodeLocationMixin) (Object) cir.getReturnValue()).setVecLocation(VecHelper.readNBT(vec));
		}
	}
	
	@Inject(method = "receive", at = @At("RETURN"), remap = false)
	private static void onReceive(
		FriendlyByteBuf buffer,
		DimensionPalette dimensions,
		CallbackInfoReturnable<TrackNodeLocation> cir
	) {
		byte[] array = new byte[3];
		buffer.readBytes(array);
		((TrackNodeLocationMixin) (Object) cir.getReturnValue()).railx$location =
			TrackNodeLocationDelta.fromByteArray(cir.getReturnValue(), array);
	}
}
