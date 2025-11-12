package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(TrackTargetingBlockItem.class)
public class TrackTargetingBlockItemMixin {
	@Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan" +
		"(Lnet/minecraft/core/Vec3i;D)Z"))
	boolean checkDistance(BlockPos instance, Vec3i to, double v, UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		if(instance != stack.get(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS))
			return instance.closerThan(to, v);
		
		boolean bezier = stack.has(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
		return instance.closerThan(
			to,
			bezier ? RailXConfig.Server.Value.getTrackTargetingMaxDistance().getAsInt() + 16 : 16
		);
	}
}
