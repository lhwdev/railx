package com.lhwdev.minecraft.railx.mixin.middleTrack;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(TrackTargetingClient.class)
public class TrackTargetingClientMixin {
	@Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has" +
		"(Lnet/minecraft/core/component/DataComponentType;)Z", ordinal = 0))
	private static boolean preventClientTickIfMiddle(ItemStack instance, DataComponentType<?> type) {
		if(type != AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS) { // some error
			return instance.has(type);
		}
		
		BlockPos hovered = instance.get(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
		if(hovered == null) return false;
		
		Level level = Minecraft.getInstance().level;
		if(level == null) return false;
		
		return level.isLoaded(hovered);
	}
}
