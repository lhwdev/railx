package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.mixin.TrackBlockItemMixinHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(TrackBlockItem.class)
public class TrackBlockItemMixin {
	@WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track" +
		"/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;" +
		"Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;" +
		"Lnet/minecraft/world/item/ItemStack;ZZ)" +
		"Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;"))
	TrackPlacement.PlacementInfo tryConnectTracks(
		Level level, Player player, BlockPos pos2, BlockState state2,
		ItemStack stack, boolean girder, boolean maximiseTurn,
		Operation<TrackPlacement.PlacementInfo> original
	) {
		var info = TrackBlockItemMixinHelper.INSTANCE
			.tryConnectFromCreateTrackItem(level, player, pos2, state2, stack, girder, maximiseTurn);
		if(info != null) {
			return info;
		} else {
			return original.call(level, player, pos2, state2, stack, girder, maximiseTurn);
		}
	}
}
