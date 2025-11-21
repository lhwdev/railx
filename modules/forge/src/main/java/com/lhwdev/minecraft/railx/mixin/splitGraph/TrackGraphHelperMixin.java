package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphTrack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;


@Mixin(TrackGraphHelper.class)
public class TrackGraphHelperMixin {
	@Inject(method = "getGraphLocationAt", at = @At("HEAD"), cancellable = true)
	private static void getGraphLocationForSplit(
		Level level,
		BlockPos pos,
		Direction.AxisDirection targetDirection,
		Vec3 targetAxis,
		CallbackInfoReturnable<TrackGraphLocation> cir
	) {
		var state = level.getBlockState(pos);
		if(state.getBlock() instanceof SplitGraphTrack split) {
			cir.setReturnValue(split.getGraphLocation(level, pos, targetDirection, targetAxis));
		}
	}
	
	@WrapOperation(method = "getBezierGraphLocationAt", at = @At(value = "INVOKE", target = "Lcom/simibubi/create" +
		"/content" +
		"/trains/track/ITrackBlock;getConnected(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;" +
		"Lnet/minecraft/world/level/block/state/BlockState;" +
		"ZLcom/simibubi/create/content/trains/graph/TrackNodeLocation;)Ljava/util/Collection;"))
	private static Collection<TrackNodeLocation.DiscoveredLocation> getConnectedForTargetLoc(
		ITrackBlock instance,
		BlockGetter worldIn,
		BlockPos pos,
		BlockState state,
		boolean linear,
		@Nullable TrackNodeLocation connectedTo,
		Operation<Collection<TrackNodeLocation.DiscoveredLocation>> original
	) {
		if(state.getBlock() instanceof SplitGraphTrack track)
			return original.call(track, worldIn, pos, state, linear, connectedTo);
		return original.call(instance, worldIn, pos, state, linear, connectedTo);
	}
}
