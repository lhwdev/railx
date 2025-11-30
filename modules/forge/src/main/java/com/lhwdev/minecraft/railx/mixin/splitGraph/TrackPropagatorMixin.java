package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.SplitTrackPropagator;
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphTrack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackPropagator;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;


@SuppressWarnings("DataFlowIssue")
@Mixin(TrackPropagator.class)
abstract class TrackPropagatorMixin {
	@WrapOperation(method = "onRailRemoved", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/track/ITrackBlock;getConnected(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;" +
		"Lnet/minecraft/world/level/block/state/BlockState;" +
		"ZLcom/simibubi/create/content/trains/graph/TrackNodeLocation;)Ljava/util/Collection;",
		ordinal = 0, remap = false), remap = false)
	private static Collection<TrackNodeLocation.DiscoveredLocation> getConnectedForCleanup(
		ITrackBlock track,
		BlockGetter worldIn,
		BlockPos pos,
		BlockState state,
		boolean linear,
		TrackNodeLocation connectedTo,
		Operation<Collection<TrackNodeLocation.DiscoveredLocation>> original
	) {
		if(track instanceof SplitGraphTrack splitTrack) {
			return splitTrack.getConnectedForCleanup(worldIn, pos, state);
		}
		return original.call(track, worldIn, pos, state, linear, connectedTo);
	}
	
	@WrapOperation(method = "onRailRemoved", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/track/TrackPropagator;onRailAdded(Lnet/minecraft/world/level/LevelAccessor;" +
		"Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)" +
		"Lcom/simibubi/create/content/trains/graph/TrackGraph;", remap = false), remap = false)
	private static TrackGraph getGraphForSplit(
		LevelAccessor reader,
		BlockPos pos,
		BlockState state,
		Operation<TrackGraph> original,
		@Local(ordinal = 1) Set<TrackGraph> toUpdate
	) {
		if(state.getBlock() instanceof SplitGraphTrack) {
			var result = SplitTrackPropagator.INSTANCE.onRailAdded(reader, pos, state);
			toUpdate.add(result.getFirst());
			toUpdate.add(result.getSecond());
			return null;
		}
		return original.call(reader, pos, state);
	}
	
	
	@Inject(method = "onRailAdded", at = @At("HEAD"), cancellable = true, remap = false)
	private static void beforeRailAdded(
		LevelAccessor reader,
		BlockPos pos,
		BlockState state,
		CallbackInfoReturnable<TrackGraph> cir
	) {
		if(state.getBlock() instanceof SplitGraphTrack) {
			SplitTrackPropagator.INSTANCE.onRailAdded(reader, pos, state);
			cir.setReturnValue(null);
		}
	}
	
	@Inject(method = "onRailAdded", at = @At(value = "INVOKE", target = "Ljava/util/Set;size()I", ordinal = 1, remap = false), remap = false)
	private static void preMergeGraph(
		CallbackInfoReturnable<TrackGraph> cir,
		@Local(ordinal = 1) LocalRef<Set<TrackGraph>> connectedGraphs
	) {
		var result = new TreeSet<TrackGraph>(Comparator.comparingInt(a -> -a.getNodes().size()));
		result.addAll(connectedGraphs.get());
		connectedGraphs.set(result);
	}
	
	@Redirect(method = "onRailAdded", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track" +
		"/ITrackBlock;walkConnectedTracks(Lnet/minecraft/world/level/BlockGetter;" +
		"Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;Z)Ljava/util/Collection;", remap = false), remap = false)
	private static Collection<TrackNodeLocation.DiscoveredLocation> walkConnectedTracksForRemove(
		BlockGetter worldIn,
		TrackNodeLocation location,
		boolean linear,
		@Share("invalidSplits") LocalRef<LongArraySet> invalidSplits
	) {
		BlockGetter world = location != null && worldIn instanceof ServerLevel sl ? sl.getServer()
			.getLevel(location.dimension) : worldIn;
		var list = new ArrayList<TrackNodeLocation.DiscoveredLocation>();
		for(BlockPos pos : location.allAdjacent()) {
			var state = world.getBlockState(pos);
			var block = state.getBlock();
			if(!(block instanceof ITrackBlock track)) continue;
			
			if(block instanceof SplitGraphTrack) {
				var invalidSet = invalidSplits.get();
				if(invalidSet == null) {
					invalidSet = new LongArraySet();
					invalidSplits.set(invalidSet);
				}
				invalidSet.add(pos.asLong());
			}
			list.addAll(track.getConnected(world, pos, state, linear, location));
		}
		return list;
	}
	
	@Inject(method = "onRailAdded", at = @At("TAIL"), remap = false)
	private static void afterRailAdded(
		LevelAccessor reader,
		BlockPos pos,
		BlockState state,
		CallbackInfoReturnable<TrackGraph> cir,
		@Share("invalidSplits") LocalRef<LongArraySet> invalidSplits
	) {
		var invalidSet = invalidSplits.get();
		if(invalidSet == null) return;
		var splitPos = new BlockPos.MutableBlockPos();
		for(long invalidPosLong : invalidSet) {
			splitPos.set(invalidPosLong);
			SplitTrackPropagator.INSTANCE.ensureSplitTrackNode(reader, splitPos, reader.getBlockState(splitPos));
		}
	}
}
