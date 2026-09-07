package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.lhwdev.minecraft.railx.common.carriageTilt.OrientedContraptionEntityWithRoll;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.createmod.catnip.math.AngleHelper.angleLerp;


@Mixin(OrientedContraptionEntity.class)
public abstract class OrientedContraptionEntityMixin extends AbstractContraptionEntity implements OrientedContraptionEntityWithRoll {
	public OrientedContraptionEntityMixin(EntityType<?> entityTypeIn, Level worldIn) {super(entityTypeIn, worldIn);}
	
	@Unique
	private float railx$prevRoll;
	
	@Unique
	private float railx$roll;
	
	
	@Override
	public float getRailx$prevRoll() {
		return railx$prevRoll;
	}
	
	@Override
	public void setRailx$prevRoll(float v) {
		railx$prevRoll = v;
	}
	
	@Override
	public float getRailx$roll() {
		return railx$roll;
	}
	
	@Override
	public void setRailx$roll(float v) {
		railx$roll = v;
	}
	
	@Override
	public float getRailx$viewZRot(float partialTicks) {
		return partialTicks == 1.0F ? railx$roll : angleLerp(partialTicks, railx$prevRoll, railx$roll);
	}
	
	@Inject(method = "getRotationState", at = @At("RETURN"))
	void getRotationStateWithRoll(CallbackInfoReturnable<AbstractContraptionEntity.ContraptionRotationState> cir) {
		var crs = cir.getReturnValue();
		crs.xRotation = railx$roll;
	}
	
	@Inject(method = "applyRotation", at = @At("RETURN"), cancellable = true)
	void applyRollToRotation(Vec3 localPos, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
		Vec3 newLocalPos = cir.getReturnValue();
		newLocalPos = VecHelper.rotate(newLocalPos, getRailx$viewZRot(partialTicks), Direction.Axis.X);
		cir.setReturnValue(newLocalPos);
	}
	
	@ModifyVariable(method = "applyRotation", at = @At("HEAD"), index = 1, argsOnly = true)
	Vec3 reverseRollFromRotation(Vec3 localPos, @Local(index = 2, argsOnly = true) float partialTicks) {
		return VecHelper.rotate(localPos, getRailx$viewZRot(partialTicks), Direction.Axis.X);
	}
	
	
	@Inject(method = "writeAdditional", at = @At("RETURN"))
	void writeRoll(CompoundTag compound, HolderLookup.Provider registries, boolean spawnPacket, CallbackInfo ci) {
		compound.putFloat("railx:Roll", railx$roll);
	}
	
	@Inject(method = "readAdditional", at = @At("RETURN"))
	void readRoll(CompoundTag compound, boolean spawnPacket, CallbackInfo ci) {
		railx$roll = compound.getFloat("railx:Roll");
	}
}
