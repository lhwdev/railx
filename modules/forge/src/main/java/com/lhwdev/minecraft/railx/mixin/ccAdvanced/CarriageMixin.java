package com.lhwdev.minecraft.railx.mixin.ccAdvanced;


import com.lhwdev.minecraft.railx.ccAdvanced.CarriageMetadata;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Carriage.class)
public class CarriageMixin {
	@Unique
	private CarriageMetadata railx$carriageMetadata;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	void onInitialize(CallbackInfo ci) {
		railx$carriageMetadata = new CarriageMetadata();
	}
	
	
	@Inject(method = "read", at = @At("RETURN"))
	private static void onRead(
		CompoundTag tag,
		TrackGraph graph,
		DimensionPalette dimensions,
		CallbackInfoReturnable<Carriage> cir
	) {
		((CarriageMixin) (Object) cir.getReturnValue()).railx$carriageMetadata.readInline(tag);
	}
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(CallbackInfoReturnable<CompoundTag> cir) {
		railx$carriageMetadata.writeInline(cir.getReturnValue());
	}
}
