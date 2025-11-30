package com.lhwdev.minecraft.railx.mixin.middleTrack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(TrackTargetingClient.class)
public class TrackTargetingClientMixin {
	@WrapOperation(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;" +
		"contains(Ljava/lang/String;)Z", ordinal = 0, remap = true), remap = false)
	private static boolean preventClientTickIfMiddle(CompoundTag instance, String key, Operation<Boolean> original) {
		if(!key.equals("SelectedPos")) { // some error
			return original.call(instance, key);
		}
		
		if(!original.call(instance, key)) return false;
		var hoveredTag = instance.get("SelectedPos");
		if(hoveredTag == null) return false;
		BlockPos hovered = NbtUtils.readBlockPos((CompoundTag) hoveredTag);
		
		Level level = Minecraft.getInstance().level;
		if(level == null) return false;
		
		return level.isLoaded(hovered);
	}
}
