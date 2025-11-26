package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphSyncPacket;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.createmod.catnip.data.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;


@Mixin(TrackGraphSyncPacket.class)
public class TrackGraphSyncPacketMixin implements SplitTrackGraphSyncPacket {
	@Unique
	private @Nullable UUID railx$connectedId;
	
	@Override
	public void railx$setConnectedId(@Nullable UUID id) {
		railx$connectedId = id;
	}
	
	@Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
	void onRead(FriendlyByteBuf buffer, CallbackInfo ci) {
		if(buffer.readBoolean())
			railx$connectedId = buffer.readUUID();
	}
	
	@WrapOperation(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At(value = "INVOKE", target =
		"Lnet" +
			"/createmod/catnip/data/Pair;of(Ljava/lang/Object;Ljava/lang/Object;)Lnet/createmod/catnip/data/Pair;",
		ordinal = 0))
	Pair<TrackNodeLocation, Vec3> onReadNode(
		Object first,
		Object second,
		Operation<Pair<TrackNodeLocation, Vec3>> original,
		@Local(argsOnly = true) FriendlyByteBuf buffer
	) {
		var result = original.call(first, second);
		if(!buffer.readBoolean()) return result;
		
		var location = (TrackNodeLocation) result.getFirst();
		var normal = (Vec3) result.getSecond();
		return new SlotObjects.TrackGraphSyncPacketSplitNodePair(
			SplittingTrackNode.Data.read(buffer), location, normal);
	}
	
	
	@Inject(method = "write", at = @At("TAIL"))
	void onWrite(FriendlyByteBuf buffer, CallbackInfo ci) {
		if(railx$connectedId != null) {
			buffer.writeBoolean(true);
			buffer.writeUUID(railx$connectedId);
		} else buffer.writeBoolean(false);
	}
	
	@Inject(method = "lambda$write$4", at = @At("TAIL"))
	private static void onWriteNode(
		FriendlyByteBuf buffer,
		Pair<?, ?> loc,
		CallbackInfo ci
	) {
		if(loc instanceof SlotObjects.TrackGraphSyncPacketSplitNodePair pair) {
			buffer.writeBoolean(true);
			pair.getData().write(buffer);
		} else {
			buffer.writeBoolean(false);
		}
	}
	
	
	@Inject(method = "handle", at = @At("TAIL"))
	void onHandle(GlobalRailwayManager manager, TrackGraph graph, CallbackInfo ci) {
		if(railx$connectedId != null) {
			((TrackGraphForSplit) graph).railx$setConnectedId(
				railx$connectedId.equals(SplitTrackGraphSync.NullConnectedId) ? null : railx$connectedId
			);
		}
	}
	
	@WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph" +
		"/TrackGraph;loadNode(Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;" +
		"ILnet/minecraft/world/phys/Vec3;)V"))
	void onLoadNode(
		TrackGraph instance,
		TrackNodeLocation location,
		int netId,
		Vec3 normal,
		Operation<Void> original,
		@Local Pair<?, ?> nodeLocation
	) {
		if(nodeLocation instanceof SlotObjects.TrackGraphSyncPacketSplitNodePair pair) {
			((TrackGraphForSplit) instance).railx$loadSplittingNode(pair.getData(), location, netId, normal);
		} else {
			original.call(instance, location, netId, normal);
		}
	}
}
