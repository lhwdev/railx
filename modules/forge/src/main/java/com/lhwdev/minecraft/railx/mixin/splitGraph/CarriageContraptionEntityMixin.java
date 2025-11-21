package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.google.common.collect.Iterables;
import com.lhwdev.minecraft.railx.splitGraph.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSyncData;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Mixin(CarriageContraptionEntity.class)
public abstract class CarriageContraptionEntityMixin extends OrientedContraptionEntity {
	@Unique
	private static EntityDataAccessor<Optional<SplitGraphTrainSync.MergedInfo>> MERGED_TRACK_GRAPH;
	
	
	@SuppressWarnings("WrongEntityDataParameterClass")
	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void onStaticInitialize(CallbackInfo ci) {
		MERGED_TRACK_GRAPH = SynchedEntityData.defineId(
			CarriageContraptionEntity.class,
			SplitGraphTrainSync.MergedInfo.SERIALIZER
		);
	}
	
	
	public CarriageContraptionEntityMixin(EntityType<?> type, Level world) {super(type, world);}
	
	@Shadow private Carriage carriage;
	
	@Shadow
	protected abstract void updateTrackGraph();
	
	@Shadow @Final private static EntityDataAccessor<Optional<UUID>> TRACK_GRAPH;
	
	
	@Unique
	private int railx$pendingGraphUpdateAt;
	
	@Unique private TrackGraph railx$connectedGraph;
	@Unique private List<UUID> railx$previousConnected;
	
	
	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(MERGED_TRACK_GRAPH, Optional.empty());
	}
	
	@Unique
	private TrackGraph railx$getConnectedGraph() {
		var graph = carriage.train.graph;
		if(graph == null) return null;
		if(TrackGraphForSplitUtils.getConnectedGraphs(graph).isEmpty()) return graph;
		if(railx$connectedGraph != null && graph.id.equals(railx$connectedGraph.id) &&
			Iterables.elementsEqual(TrackGraphForSplitUtils.getConnectedGraphs(graph), railx$previousConnected)) {
			return railx$connectedGraph;
		}
		
		var result = new ConnectedTrackGraphs(CreateClient.RAILWAYS, graph);
		railx$connectedGraph = result;
		railx$previousConnected = new ArrayList<>(TrackGraphForSplitUtils.getConnectedGraphs(graph));
		return result;
	}
	
	@WrapOperation(method = "onSyncedDataUpdated", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/entity/CarriageSyncData;apply(Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;" +
		"Lcom/simibubi/create/content/trains/entity/Carriage;)V"))
	void onCarriageDataUpdated(
		CarriageSyncData instance,
		CarriageContraptionEntity entity,
		Carriage carriage,
		Operation<Void> original
	) {
		((CarriageSyncDataForSplit) instance).railx$setConnectedGraph(railx$getConnectedGraph());
		original.call(instance, entity, carriage);
	}
	
	@Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
	void onSyncedDataUpdated(EntityDataAccessor<?> key, CallbackInfo ci) {
		if(MERGED_TRACK_GRAPH.equals(key))
			updateTrackGraph();
	}
	
	@Inject(method = "tickContraption", at = @At(value = "RETURN", ordinal = 3))
	void tickContraptionServer(CallbackInfo ci) {
		// no-op: if(carriage == null) return; // after if(carriage == null) ... return
		// no-op: if(level().isClientSide) return; // return(ordinal=3) is inside if(!level.isClientSide))
		SplitGraphTrainSync.MergedInfo info = null;
		var graph = carriage.train.graph;
		if(graph instanceof MergedTrackGraph merged)
			info = new SplitGraphTrainSync.MergedInfo(merged.getGraphs().stream().map(g -> g.id).toList());
		entityData.set(MERGED_TRACK_GRAPH, Optional.ofNullable(info));
	}
	
	@Inject(method = "tickContraption", at = @At(value = "RETURN", ordinal = 5))
	void tickContraptionClient(CallbackInfo ci) {
		if(railx$pendingGraphUpdateAt <= tickCount) {
			updateTrackGraph();
			railx$pendingGraphUpdateAt = Integer.MAX_VALUE;
		}
	}
	
	@WrapMethod(method = "updateTrackGraph")
	void updateTrackGraph(Operation<Void> original) {
		if(carriage == null) return;
		var info = entityData.get(MERGED_TRACK_GRAPH).orElse(null);
		var current = carriage.train.graph;
		if(current == null || info == null || entityData.get(TRACK_GRAPH).isEmpty()) {
			original.call();
			return;
		}
		
		var manager = CreateClient.RAILWAYS;
		var graphs = new ArrayList<TrackGraph>();
		for(var graphId : info.getGraphs()) {
			var graph = manager.trackNetworks.get(graphId);
			if(graph == null) continue;
			graphs.add(graph);
		}
		if(!graphs.isEmpty())
			carriage.train.graph = new MergedTrackGraphImpl(graphs);
		carriage.train.derailed = false;
	}
	
	@Inject(method = "setCarriage", at = @At("RETURN"))
	void setCarriage(Carriage carriage, CallbackInfo ci) {
		var graph = carriage.train.graph;
		if(graph instanceof MergedTrackGraph merged) {
			var info = new SplitGraphTrainSync.MergedInfo(merged.getGraphs().stream().map(g -> g.id).toList());
			entityData.set(MERGED_TRACK_GRAPH, Optional.of(info));
		}
	}
}
