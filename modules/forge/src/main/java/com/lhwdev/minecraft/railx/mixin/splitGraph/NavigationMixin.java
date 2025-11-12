package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.*;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.station.GlobalStation;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Mixin(Navigation.class)
class NavigationMixin implements SplittingNavigation {
	@Shadow public Train train;
	@Shadow public GlobalStation destination;
	@Shadow List<Couple<TrackNode>> currentPath;
	
	
	@Unique
	private MergedTrackGraph railx$currentPathGraph;
	
	@Override
	public @Nullable TrackGraph railx$currentPathGraph() {
		return railx$currentPathGraph;
	}
	
	@Redirect(method = "<init>", at = @At(value = "NEW", target = "()Lcom/simibubi/create/content/trains/entity" +
		"/TravellingPoint;"))
	TravellingPoint createSignalScout() {
		return new TravellingPointForSplitScout();
	}
	
	@Redirect(method = "tick", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Train;" +
		"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;", ordinal = 1))
	TrackGraph trackGraphForTick(Train train) {
		if(railx$currentPathGraph == null) return train.graph;
		return railx$currentPathGraph;
	}
	
	@Redirect(method = "currentSignalResolved", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content" +
		"/trains/entity/Train;" +
		"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;", ordinal = 0))
	TrackGraph trackGraphForCurrentSignalResolved(Train train) {
		if(railx$currentPathGraph == null) return train.graph;
		return railx$currentPathGraph;
	}
	
	@ModifyVariable(method = "navigateOptions", at = @At("HEAD"), index = 2, argsOnly = true)
	TrackGraph trackGraphForNavigateOptions(TrackGraph previous) {
		if(railx$currentPathGraph == null) return previous;
		return railx$currentPathGraph;
	}
	
	@Inject(method = "startNavigation", at = @At("RETURN"))
	void updatePathGraph(DiscoveredPath pathTo, CallbackInfoReturnable<Double> cir) {
		if(pathTo instanceof SlotObjects.SplitDiscoveredPath splitPath) {
			railx$currentPathGraph = new MergedTrackGraphImpl(splitPath.getGraphs());
		}
	}
	
	@Inject(method = "cancelNavigation", at = @At("RETURN"))
	void onCancelNavigation(CallbackInfo ci) {
		railx$currentPathGraph = null;
	}
	
	@Redirect(method = "findPathTo(Ljava/util/ArrayList;D)Lcom/simibubi/create/content/trains/graph/DiscoveredPath;",
		at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Train;" +
			"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;", ordinal = 0))
	TrackGraph trackGraphForFindPathTo(Train train) {
		var graph = train.graph;
		if(graph == null) return null;
		if(graph instanceof AllConnectedTrackGraphs merged) graph = merged.getBase(); // temporary
		return new PropagatingTrackGraph(Create.RAILWAYS, graph);
	}
	
	@Redirect(method = "findPathTo(Ljava/util/ArrayList;D)" +
		"Lcom/simibubi/create/content/trains/graph/DiscoveredPath;", at = @At(value = "INVOKE", target = "Lcom" +
		"/simibubi/create/content/trains/entity/Navigation;search(DDZLjava/util/ArrayList;" +
		"Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V"))
	void hi(
		Navigation instance,
		double maxDistance,
		double maxCost,
		boolean forward,
		ArrayList<GlobalStation> destinations,
		Navigation.StationTest stationTest,
		@Local(index = 4) TrackGraph graph
	) {
		var previous = train.graph;
		train.graph = graph;
		try {
			instance.search(maxDistance, maxCost, forward, destinations, stationTest);
		} finally {
			train.graph = previous;
		}
	}
	
	// TODO: currently StationPredicate is not usable because of 'Frontier.remaining' distance calculation to station.
	/*@Inject(method = "lambda$findPathTo$5", at = @At("HEAD"), cancellable = true)
	private static void findPathTo$stationTest(
		ArrayList<GlobalStation> destinations,
		TrackEdge initialEdge,
		TrackGraph graph,
		Couple<DiscoveredPath> results,
		boolean forward,
		double distance,
		double cost,
		Map<TrackEdge, Pair<Boolean, Couple<TrackNode>>> reachedVia,
		Pair<Couple<TrackNode>, TrackEdge> currentEntry,
		GlobalStation globalStation,
		CallbackInfoReturnable<Boolean> cir
	) {
		if(!(destinations instanceof SlotObjects.StationPredicate predicate)) return;
		if(!predicate.getCondition().invoke(globalStation)) {
			cir.setReturnValue(false);
			return;
		}
		TrackEdge edge = currentEntry.getSecond();
		TrackNode node1 = currentEntry.getFirst()
			.getFirst();
		TrackNode node2 = currentEntry.getFirst()
			.getSecond();
		
		List<Couple<TrackNode>> currentPath = new ArrayList<>();
		Pair<Boolean, Couple<TrackNode>> backTrack = reachedVia.get(edge);
		Couple<TrackNode> toReach = Couple.create(node1, node2);
		TrackEdge edgeReached = edge;
		while(backTrack != null) {
			if(edgeReached == initialEdge)
				break;
			if(backTrack.getFirst())
				currentPath.addFirst(toReach);
			toReach = backTrack.getSecond();
			edgeReached = graph.getConnection(toReach);
			backTrack = reachedVia.get(edgeReached);
		}
		
		double position = edge.getLength() - globalStation.getLocationOn(edge);
		double distanceToDestination = distance - position;
		results.set(
			forward,
			new DiscoveredPath((forward ? 1 : -1) * distanceToDestination, cost, currentPath, globalStation)
		);
		cir.setReturnValue(true);
	}*/
	
	@Redirect(method = "lambda$findPathTo$5", at = @At(value = "NEW", target = "(DDLjava/util/List;" +
		"Lcom/simibubi/create/content/trains/station/GlobalStation;)" +
		"Lcom/simibubi/create/content/trains/graph/DiscoveredPath;"))
	private static DiscoveredPath createFoundPath(
		double distance,
		double cost,
		List<Couple<TrackNode>> path,
		GlobalStation destination,
		@Local(index = 1, argsOnly = true) TrackEdge initialEdge,
		@Local(index = 2, argsOnly = true) TrackGraph graph,
		@Local(index = 9, argsOnly = true) Map<TrackEdge, Pair<Boolean, Couple<TrackNode>>> reachedVia,
		@Local(index = 10, argsOnly = true) Pair<Couple<TrackNode>, TrackEdge> currentEntry
	) {
		if(TrackGraphForSplitUtils.getConnectedGraphs(graph).isEmpty())
			return new DiscoveredPath(distance, cost, path, destination);
		
		var graphs = new ReferenceArraySet<TrackGraph>();
		graphs.add(MergedTrackGraph.getBase(graph));
		
		var currentEdge = currentEntry.getSecond();
		while(currentEdge != initialEdge) {
			var backTrack = reachedVia.get(currentEdge);
			if(backTrack == null) break;
			var toReach = backTrack.getSecond();
			if(toReach.getSecond() instanceof SplittingTrackNode split) {
				var otherGraph = Create.RAILWAYS.trackNetworks.get(split.getOtherGraph());
				if(otherGraph != null) graphs.add(otherGraph);
			}
			currentEdge = graph.getConnection(toReach);
		}
		
		return new SlotObjects.SplitDiscoveredPath(distance, cost, path, destination, graphs);
	}
	
	@Redirect(method = "search(DDZLjava/util/ArrayList;" +
		"Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "FIELD", target =
		"Lcom/simibubi/create/content/trains/entity/Train;graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;",
		ordinal = 0))
	TrackGraph trackGraphForSearch(Train train) {
		var graph = train.graph;
		if(graph == null) return null;
		if(graph instanceof PropagatingTrackGraph)
			return graph; // called from findPathTo; reusing graph to provide graphs to backTracking
		if(graph instanceof AllConnectedTrackGraphs merged) graph = merged.getBase(); // temporary
		return new PropagatingTrackGraph(Create.RAILWAYS, graph);
	}
	
	@Definition(id = "otherTrain", local = @Local(type = Train.class, index = 13))
	@Definition(id = "fGraph", field = "Lcom/simibubi/create/content/trains/entity/Train;" +
		"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;")
	@Definition(id = "graph", local = @Local(type = TrackGraph.class, index = 8))
	@Expression("otherTrain.fGraph != graph")
	@ModifyExpressionValue(method = "search(DDZLjava/util/ArrayList;" +
		"Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
	boolean isTrainNotReachable(boolean original, @Local(index = 13) Train otherTrain) {
		return !TrackGraphConnectedIdUtils.isReachableTo(train, otherTrain);
	}
	
	@Redirect(method = "search(DDZLjava/util/ArrayList;" +
		"Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target =
		"Lcom/simibubi/create/content/trains/entity/Train;getEndpointEdges()Lnet/createmod/catnip/data/Couple;"))
	Couple<Couple<TrackNode>> getEndpointEdgesForTrainCost(Train instance) {
		return Couple.create(
			instance.carriages.getFirst().getLeadingPoint(),
			instance.carriages.getLast().getTrailingPoint()
		).map(tp -> {
			var graph = ((TravellingPointForSplit) tp).getDestinationGraph();
			if(graph == null) graph = this.train.graph;
			return new SlotObjects.TrainEndpointEdge(graph, tp);
		});
	}
	
	@Redirect(method = "lambda$search$7", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/graph/TrackGraph;getConnection(Lnet/createmod/catnip/data/Couple;)" +
		"Lcom/simibubi/create/content/trains/graph/TrackEdge;"))
	private static TrackEdge getConnectionForTrainCost(
		TrackGraph instance,
		Couple<TrackNode> nodes,
		@Local(index = 3, argsOnly = true) Couple<TrackNode> endpointEdge,
		@Local(index = 7) boolean flip
	) {
		var endpoint = (SlotObjects.TrainEndpointEdge) endpointEdge;
		return flip ? endpoint.getEdge2() : endpoint.getEdge();
	}
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
		if(destination != null && railx$currentPathGraph != null) {
			var graphs = railx$currentPathGraph.getGraphs();
			var graphsIt = graphs.iterator();
			var bytes = new long[graphs.size() * 2];
			for(int i = 0; i < bytes.length; i += 2) {
				var uuid = graphsIt.next().id;
				bytes[i] = uuid.getMostSignificantBits();
				bytes[i + 1] = uuid.getLeastSignificantBits();
			}
			cir.getReturnValue().putLongArray("railx:PathGraphs", bytes);
		}
	}
	
	@Inject(method = "read", at = @At("HEAD"))
	void onRead(CompoundTag tag, TrackGraph graph, DimensionPalette dimensions, CallbackInfo ci) {
		if(tag.contains("railx:PathGraphs") && graph != null) {
			var manager = Create.RAILWAYS;
			var bytes = tag.getLongArray("railx:PathGraphs");
			var graphs = new ArrayList<TrackGraph>();
			for(int i = 0; i < bytes.length; i += 2) {
				var uuid = new UUID(bytes[i], bytes[i + 1]);
				var pathGraph = manager.trackNetworks.get(uuid);
				if(pathGraph != null) graphs.add(pathGraph);
			}
			if(!graphs.isEmpty()) railx$currentPathGraph = new MergedTrackGraphImpl(graphs);
		}
	}
	
	@ModifyVariable(method = "read", at = @At("HEAD"), index = 2, argsOnly = true)
	TrackGraph getGraphForRead(TrackGraph graph) {
		if(graph == null) return null;
		return new AllConnectedTrackGraphs(Create.RAILWAYS, graph);
	}
	
	@ModifyVariable(method = "read", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift =
		At.Shift.AFTER), index = 2, argsOnly = true)
	TrackGraph updatePathGraphForRead(TrackGraph graph) {
		var networks = Create.RAILWAYS.trackNetworks;
		if(networks.isEmpty()) {
			var sd = RailwayServerGlobals.INSTANCE.getSavedData();
			if(sd != null) networks = sd.getTrackNetworks();
		}
		return new PropagatingTrackGraphForNavigationRead(networks, graph);
	}
}
