package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.graph.*;
import net.createmod.catnip.data.Pair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackGraphSync.class)
public abstract class TrackGraphSyncMixin implements SplitTrackGraphSync {
	@Shadow(remap = false)
	protected abstract void flushGraphPacket(TrackGraph graph);
	
	@Shadow(remap = false)
	private TrackGraphSyncPacket currentGraphSyncPacket;
	
	@Override
	public void railx$connectedIdChanged(@NotNull TrackGraph graph) {
		flushGraphPacket(graph);
		((SplitTrackGraphSyncPacket) currentGraphSyncPacket)
			.railx$setConnectedId(TrackGraphForSplitUtils.getConnectedId(graph));
	}
	
	@Inject(method = "sendFullGraphTo", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains" +
		"/graph" +
		"/TrackGraphSyncPacket;fullWipe:Z", shift = At.Shift.AFTER, remap = false), remap = false)
	void sendFullGraphTo(TrackGraph graph, ServerPlayer player, CallbackInfo ci, @Local TrackGraphSyncPacket packet) {
		((SplitTrackGraphSyncPacket) packet)
			.railx$setConnectedId(TrackGraphForSplitUtils.getConnectedId(graph));
	}
	
	@WrapOperation(method = "nodeAdded", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Pair;of" +
		"(Ljava/lang/Object;Ljava/lang/Object;)Lnet/createmod/catnip/data/Pair;", ordinal = 0, remap = false), remap = false)
	Pair<?, ?> addNodeToNodeAdded(
		Object location,
		Object normal,
		Operation<Pair<?, ?>> original,
		@Local(argsOnly = true) TrackNode node
	) {
		var result = original.call(location, normal);
		if(node instanceof SplittingTrackNode split)
			return new SlotObjects.TrackGraphSyncPacketSplitNodePair(
				split.data(),
				(TrackNodeLocation) result.getFirst(),
				(Vec3) result.getSecond()
			);
		return result;
	}
	
	@Redirect(method = "sendFullGraphTo", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Pair;of" +
		"(Ljava/lang/Object;Ljava/lang/Object;)Lnet/createmod/catnip/data/Pair;", ordinal = 0, remap = false), remap = false)
	Pair<?, ?> addNodeToFullGraph(Object location, Object normal, @Local TrackNode node) {
		var result = Pair.of(location, normal);
		if(node instanceof SplittingTrackNode split)
			return new SlotObjects.TrackGraphSyncPacketSplitNodePair(
				split.data(),
				(TrackNodeLocation) result.getFirst(),
				(Vec3) result.getSecond()
			);
		return result;
	}
}
