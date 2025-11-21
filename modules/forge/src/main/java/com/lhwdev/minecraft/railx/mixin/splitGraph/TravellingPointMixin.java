package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.TravellingPointForSplit;
import com.lhwdev.minecraft.railx.splitGraph.TravellingPointForSplitHelper;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(TravellingPoint.class)
public class TravellingPointMixin implements TravellingPointForSplit {
	@Shadow public boolean blocked;
	@Shadow public TrackNode node1;
	@Shadow public TrackNode node2;
	
	@Unique private TrackGraph railx$destinationGraph;
	
	@Unique private TrackNode railx$previousNode1;
	@Unique private TrackNode railx$previousNode2;
	
	
	@Override
	public @Nullable TrackGraph getRailx$destinationGraph() {
		return railx$destinationGraph;
	}
	
	@Override
	public void setRailx$destinationGraph(@Nullable TrackGraph graph) {
		railx$destinationGraph = graph;
		railx$previousNode1 = node1;
		railx$previousNode2 = node2;
	}
	
	@Override
	public @NotNull TrackGraph railx$prepareTravel(@NotNull TrackGraph graph) {
		if(railx$previousNode1 != node1 || railx$previousNode2 != node2) {
			railx$destinationGraph = null;
			railx$previousNode1 = node1;
			railx$previousNode2 = node2;
		}
		var destinationGraph = railx$destinationGraph;
		if(destinationGraph != null) return destinationGraph;
		return graph;
	}
	
	@ModifyVariable(method = "travel(Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
		"DLcom/simibubi/create/content/trains/entity/TravellingPoint$ITrackSelector;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IEdgePointListener;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$ITurnListener;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IPortalListener;)D",
		at = @At("HEAD"), index = 1, argsOnly = true)
	TrackGraph railx$graphForTravel(TrackGraph graph) {
		return railx$prepareTravel(graph);
	}
	
	@Inject(method = "travel(Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
		"DLcom/simibubi/create/content/trains/entity/TravellingPoint$ITrackSelector;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IEdgePointListener;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$ITurnListener;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IPortalListener;)D",
		at = @At("TAIL"), cancellable = true)
	void afterTravel(
		TrackGraph graph,
		double distance,
		TravellingPoint.ITrackSelector trackSelector,
		TravellingPoint.IEdgePointListener signalListener,
		TravellingPoint.ITurnListener turnListener,
		TravellingPoint.IPortalListener portalListener,
		CallbackInfoReturnable<Double> cir
	) {
		if(!blocked) return;
		double moved = cir.getReturnValueD();
		var result = TravellingPointForSplitHelper.INSTANCE.tryTravelThroughSplit(
			(TravellingPoint) (Object) this,
			distance - moved,
			trackSelector,
			signalListener,
			turnListener,
			portalListener
		);
		cir.setReturnValue(moved + result);
	}
	
	@ModifyVariable(method = "edgeTraversedFrom", at = @At("HEAD"), index = 1, argsOnly = true)
	TrackGraph graphForEdgeTraversedFrom(TrackGraph original) {
		var destinationGraph = railx$destinationGraph;
		if(destinationGraph != null) return destinationGraph;
		return original;
	}
	
	@ModifyVariable(method = "reverse", at = @At("HEAD"), index = 1, argsOnly = true)
	TrackGraph graphForReverse(TrackGraph original) {
		var destinationGraph = railx$destinationGraph;
		if(destinationGraph != null) return destinationGraph;
		return original;
	}
	
	@ModifyVariable(method = "getPositionWithOffset", at = @At("HEAD"), index = 1, argsOnly = true)
	TrackGraph graphForGetPositionWithOffset(TrackGraph original) {
		var destinationGraph = railx$destinationGraph;
		if(destinationGraph != null) return destinationGraph;
		return original;
	}
}
