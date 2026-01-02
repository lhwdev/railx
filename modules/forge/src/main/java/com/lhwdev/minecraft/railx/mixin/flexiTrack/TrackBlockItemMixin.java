package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlaceResult;
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem;
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial;
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackPlacement;
import com.lhwdev.minecraft.railx.registry.AllDataComponents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(TrackBlockItem.class)
public class TrackBlockItemMixin extends BlockItem {
	public TrackBlockItemMixin(Block block, Properties properties) {super(block, properties);}
	
	@WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track" +
		"/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;" +
		"Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;" +
		"Lnet/minecraft/world/item/ItemStack;ZZ)" +
		"Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;"))
	TrackPlacement.PlacementInfo tryConnectTracks(
		Level level,
		Player player,
		BlockPos pos,
		BlockState state,
		ItemStack stack,
		boolean hasGirder,
		boolean extend,
		Operation<TrackPlacement.PlacementInfo> original,
		@Local(index = 1, argsOnly = true) UseOnContext context,
		@Cancellable CallbackInfoReturnable<InteractionResult> cir
	) {
		var isFlexible = FlexiTrackPlacement.INSTANCE.isFlexible(level, stack);
		var previousToState = level.getBlockState(pos);
		if(!isFlexible && !FlexiTrackPlacement.INSTANCE.isFlexiPlacementRequired(level, previousToState, stack))
			return original.call(level, player, pos, state, stack, hasGirder, extend);
		
		if(!(stack.getItem() instanceof TrackBlockItem item))
			return original.call(level, player, pos, state, stack, hasGirder, extend);
		
		if(!(state.getBlock() instanceof ITrackBlock)) {
			var placeContext = new BlockPlaceContext(context);
			state = isFlexible ?
				FlexiTrackBlockItem.INSTANCE.getFlexiblePlacementState(item, placeContext) :
				getPlacementState(placeContext);
		}
		if(state == null) return FlexiTrackBlockItem.INSTANCE.getStubPlacementInfo();
		
		var parameter = new FlexiTrackPlacement.Parameter(isFlexible, isFlexible || extend, hasGirder);
		FlexiPlaceResult result = FlexiTrackPlacement.INSTANCE
			.tryConnect(level, player, pos, state, stack, parameter);
		
		stack.remove(AllDataComponents.INSTANCE.getFlexiblePlacement());
		
		var error = result.getError();
		if(error != null && !level.isClientSide)
			player.displayClientMessage(error.getMessage(), true);
		if(!result.getValid()) {
			AllSoundEvents.DENY.playFrom(player, 1, 1);
			cir.setReturnValue(InteractionResult.FAIL);
		}
		return FlexiTrackBlockItem.INSTANCE.getStubPlacementInfo();
	}
	
	@ModifyArg(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;" +
		"setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"), index = 1)
	ItemStack modifyItemInHand(ItemStack stack) {
		stack.remove(AllDataComponents.INSTANCE.getFlexiblePlacement());
		return stack;
	}
	
	@Override
	protected @Nullable BlockState getPlacementState(@NotNull BlockPlaceContext context) {
		var flexible = FlexiTrackPlacement.INSTANCE.isFlexible(
			context.getLevel(),
			context.getItemInHand()
		);
		if(!flexible) return super.getPlacementState(context);
		
		if(!(getBlock() instanceof TrackBlock track)) return null;
		var flexiTrack = FlexiTrackMaterial.INSTANCE.toFlexible(track);
		if(flexiTrack == null) return super.getPlacementState(context);
		return flexiTrack.getStateForPlacement(context);
	}
	
	@Override
	protected boolean placeBlock(@NotNull BlockPlaceContext context, @NotNull BlockState state) {
		var result = super.placeBlock(context, state);
		if(result) FlexiTrackBlockItem.INSTANCE.placeBlock(context, state);
		return result;
	}
}
