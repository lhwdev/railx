package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.*;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.*;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import kotlin.collections.CollectionsKt;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@SuppressWarnings("DataFlowIssue")
@Mixin(Train.class)
public abstract class TrainMixin implements TrainForSplit {
	
	@Shadow(remap = false) public UUID id;
	@Shadow(remap = false) public Component name;
	@Shadow(remap = false) public boolean derailed;
	@Shadow(remap = false) public TrackGraph graph;
	@Shadow(remap = false) public List<Carriage> carriages;
	
	@Shadow(remap = false) public Navigation navigation;
	@Shadow(remap = false) public ScheduleRuntime runtime;
	@Shadow(remap = false) public TrainStatus status;
	
	@Shadow(remap = false) public int migrationCooldown;
	@Shadow(remap = false) List<TrainMigration> migratingPoints;
	@Shadow(remap = false) public boolean updateSignalBlocks;
	
	@Shadow(remap = false)
	public abstract GlobalStation getCurrentStation();
	
	
	@Unique
	private TrackGraph railx$graph;
	
	@Override
	public void railx$replaceGraphPreserving(@Nullable TrackGraph from, @Nullable TrackGraph to) {
		if(graph == from) {
			graph = to;
			railx$graph = to;
			return;
		}
		if(!(graph instanceof MergedTrackGraph merged)) return;
		if(merged.getGraphs().contains(from)) {
			var graphs = new ArrayList<>(merged.getGraphs());
			for(var i = 0; i < graphs.size(); i++) {
				if(graphs.get(i) == (Object) this) graphs.set(i, graph);
			}
			graph = new MutableMergedTrackGraph(graphs);
			railx$graph = graph;
		}
	}
	
	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void afterRead(CallbackInfoReturnable<Train> cir) {
		var train = cir.getReturnValue();
		((TrainMixin) (Object) train).railx$graph = train.graph;
	}
	
	@Inject(method = "tick", at = @At("RETURN"), remap = false)
	void onTick(Level level, CallbackInfo ci) {
		var previous = this.graph;
		if(previous == null || previous != railx$graph) {
			// TODO: some guessing needed?
			railx$setAllPointDestinations(null);
			railx$graph = previous;
			return;
		}
		
		var found = new ReferenceArraySet<TrackGraph>();
		for(var carriage : carriages) {
			found.add(TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getFirst().leading()));
			found.add(TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getFirst().trailing()));
			if(!carriage.isOnTwoBogeys()) continue;
			found.add(TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getSecond().leading()));
			found.add(TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getSecond().trailing()));
		}
		
		if(previous instanceof MergedTrackGraph mergedBase) {
			MutableMergedTrackGraph merged;
			if(mergedBase instanceof MutableMergedTrackGraph m) merged = m;
			else merged = new MutableMergedTrackGraph(new ArrayList<>(mergedBase.getGraphs()));
			
			if(found.size() == 1) {
				var single = found.iterator().next();
				if(single == null) single = merged.getBase(); // undefined behavior
				this.graph = single;
				this.railx$graph = single;
				railx$setAllPointDestinations(null);
				return;
			}
			var graphs = merged.getGraphs();
			
			int i;
			for(i = 0; i < graphs.size(); i++) {
				var prev = graphs.get(i);
				if(!found.contains(prev)) break;
			}
			
			if(i == graphs.size() && i == found.size()) return;
			
			var base = i == 0 ? graphs.get(0) : null;
			graphs.subList(i, graphs.size()).clear();
			for(int j = 0; j < i; j++) found.remove(graphs.get(j));
			for(var foundGraph : found) {
				if(foundGraph == null) {
					if(base != null) {
						graphs.add(0, base);
						base = null;
					}
				} else {
					graphs.add(foundGraph);
					if(foundGraph == base) base = null;
				}
			}
			if(graphs.size() == 1) this.graph = graphs.get(0);
		} else {
			if(found.size() == 1) {
				var single = found.iterator().next();
				if(single != null) {
					this.graph = single;
					this.railx$graph = single;
				}
				return;
			}
			
			var graphs = new ArrayList<TrackGraph>();
			graphs.add(previous);
			var hasNull = false;
			for(var graph : found) {
				if(graph == null) {
					hasNull = true;
					continue;
				}
				if(graph == previous) continue;
				graphs.add(graph);
			}
			if(graphs.size() == 1) {
				railx$setAllPointDestinations(null);
				this.graph = graphs.get(0); // must be previous...
				this.railx$graph = this.graph;
			}
			if(hasNull) railx$replaceAllPointDestinations(null, previous);
			this.graph = new MutableMergedTrackGraph(graphs);
			this.railx$graph = this.graph;
		}
	}
	
	@Unique
	private void railx$setAllPointDestinations(TrackGraph to) {
		for(var carriage : carriages) {
			TravelingPointSplitUtils.setDestinationGraph((carriage.bogeys.getFirst().leading()), to);
			TravelingPointSplitUtils.setDestinationGraph((carriage.bogeys.getFirst().trailing()), to);
			if(!carriage.isOnTwoBogeys()) continue;
			TravelingPointSplitUtils.setDestinationGraph((carriage.bogeys.getSecond().leading()), to);
			TravelingPointSplitUtils.setDestinationGraph((carriage.bogeys.getSecond().trailing()), to);
		}
	}
	
	@Unique
	private void railx$replaceAllPointDestinations(TrackGraph from, TrackGraph to) {
		for(var carriage : carriages) {
			if(from == TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getFirst().leading()))
				TravelingPointSplitUtils.setDestinationGraph(carriage.bogeys.getFirst().leading(), to);
			if(from == TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getFirst().trailing()))
				TravelingPointSplitUtils.setDestinationGraph(carriage.bogeys.getFirst().trailing(), to);
			if(!carriage.isOnTwoBogeys()) continue;
			if(from == TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getSecond().leading()))
				TravelingPointSplitUtils.setDestinationGraph(carriage.bogeys.getSecond().leading(), to);
			if(from == TravelingPointSplitUtils.getDestinationGraph(carriage.bogeys.getSecond().trailing()))
				TravelingPointSplitUtils.setDestinationGraph(carriage.bogeys.getSecond().trailing(), to);
		}
	}
	
	
	@ModifyExpressionValue(method = {"tickOccupiedObservers", "updateNavigationTarget", "getCurrentStation"},
		at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Train;" +
			"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;", opcode = Opcodes.GETFIELD, remap = false),
		remap = false)
	TrackGraph getPathGraph(TrackGraph original) {
		var pathGraph = ((SplittingNavigation) navigation).railx$currentPathGraph();
		if(pathGraph != null) return pathGraph;
		return original;
	}
	
	@ModifyExpressionValue(method = "collectInitiallyOccupiedSignalBlocks", at = @At(value = "FIELD", target = "Lcom" +
		"/simibubi/create/content/trains/entity/Train;graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;",
		remap = false), remap = false)
	TrackGraph getGraphForCollectInitiallyOccupiedSignalBlocks(TrackGraph original) {
		return getPathGraph(original);
	}
	
	@ModifyExpressionValue(method = "collectInitiallyOccupiedSignalBlocks", at = @At(value = "NEW", target = "(Lcom" +
		"/simibubi/create/content/trains/graph/TrackNode;Lcom/simibubi/create/content/trains/graph/TrackNode;" +
		"Lcom/simibubi/create/content/trains/graph/TrackEdge;DZ)" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint;", remap = false), remap = false)
	TravellingPoint createTravelingPointForCollectInitiallyOccupiedSignalBlocks(TravellingPoint original) {
		return new MovingTravellingPoint(original);
	}
	
	
	@Definition(id = "train", local = @Local(type = Train.class, index = 9))
	@Definition(id = "graph", field = "Lcom/simibubi/create/content/trains/entity/Train;" +
		"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;")
	@Expression("train.graph != this.graph")
	@ModifyExpressionValue(method = "findCollidingTrain", at = @At(value = "MIXINEXTRAS:EXPRESSION", remap = false),
		remap = false, require = 0)
	boolean isCollidingTrainNotReachableTo(boolean original, @Local(index = 9) Train train) {
		return !TrackGraphConnectedIdUtils.isReachableTo(train.graph, graph);
	}
	
	@Inject(method = "lambda$detachFromTracks$14", at = @At("HEAD"), cancellable = true, remap = false)
	void createTrainMigration(TravellingPoint tp, CallbackInfo ci) {
		if(!(graph instanceof MergedTrackGraph merged)) return;
		var migration = new TrainMigration(tp);
		var pointGraph = TravelingPointSplitUtils.getDestinationGraph(tp);
		((TrainMigrationForSplit) migration)
			.railx$setGraphIndex(pointGraph == null ? 0 : CollectionsKt.indexOf(merged.getGraphs(), pointGraph));
		migratingPoints.add(migration);
		ci.cancel();
	}
	
	/**
	 * @author lhwdev
	 * @reason whole overhaul
	 */
	@Inject(method = "reattachToTracks", at = @At("HEAD"), remap = false)
	public void onReattachToTracks(Level level, CallbackInfo ci) {
		if(migrationCooldown > 0) {
			migrationCooldown--;
			return;
		}
		
		var result = TrainSplitUtils.INSTANCE.reattachToTracks((Train) (Object) this, level, migratingPoints);
		if(result == null) return;
		
		var graphs = result.getGraphs();
		if(graphs.size() == 1) {
			graph = graphs.values().iterator().next();
		} else {
			graph = new MutableMergedTrackGraph(new ArrayList<>(graphs.values()));
		}
		railx$graph = graph;
		migratingPoints.clear();
		if(derailed) {
			status.successfulNavigation();
			derailed = false;
		}
		if(runtime.getSchedule() != null && runtime.state == ScheduleRuntime.State.IN_TRANSIT)
			runtime.state = ScheduleRuntime.State.PRE_TRANSIT;
		var currentStation = getCurrentStation();
		if(currentStation != null)
			currentStation.reserveFor((Train) (Object) this);
		updateSignalBlocks = true;
		migrationCooldown = 0;
		
		ci.cancel();
	}
}
