package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.*;
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
	@Shadow
	protected abstract void flushGraphPacket(TrackGraph graph);
	
	@Shadow private TrackGraphSyncPacket currentGraphSyncPacket;
	
	@Override
	public void railx$connectedIdChanged(@NotNull TrackGraph graph) {
		flushGraphPacket(graph);
		((SplitTrackGraphSyncPacket) currentGraphSyncPacket)
			.railx$setConnectedId(TrackGraphForSplitUtils.getConnectedId(graph));
	}
	
	@Inject(method = "sendFullGraphTo", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains" +
		"/graph" +
		"/TrackGraphSyncPacket;fullWipe:Z", shift = At.Shift.AFTER))
	void sendFullGraphTo(TrackGraph graph, ServerPlayer player, CallbackInfo ci, @Local TrackGraphSyncPacket packet) {
		((SplitTrackGraphSyncPacket) packet)
			.railx$setConnectedId(TrackGraphForSplitUtils.getConnectedId(graph));
	}
	
	@Redirect(method = "nodeAdded", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Pair;of" +
		"(Ljava/lang/Object;Ljava/lang/Object;)Lnet/createmod/catnip/data/Pair;", ordinal = 0))
	Pair<?, ?> addNodeToNodeAdded(Object location, Object normal, @Local(argsOnly = true) TrackNode node) {
		if(node instanceof SplittingTrackNode split)
			return new SlotObjects.TrackGraphSyncPacketSplitNodePair(
				split.data(),
				(TrackNodeLocation) location,
				(Vec3) normal
			);
		return Pair.of(location, normal);
	}
	
	@Redirect(method = "sendFullGraphTo", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Pair;of" +
		"(Ljava/lang/Object;Ljava/lang/Object;)Lnet/createmod/catnip/data/Pair;", ordinal = 0))
	Pair<?, ?> addNodeToFullGraph(Object location, Object normal, @Local TrackNode node) {
		if(node instanceof SplittingTrackNode split)
			return new SlotObjects.TrackGraphSyncPacketSplitNodePair(
				split.data(),
				(TrackNodeLocation) location,
				(Vec3) normal
			);
		return Pair.of(location, normal);
	}
}
