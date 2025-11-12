//package com.lhwdev.minecraft.railx.mixin.splitGraph;
//
//import com.lhwdev.minecraft.railx.splitGraph.RailwayServerGlobals;
//import com.lhwdev.minecraft.railx.splitGraph.TravellingPointForSplit;
//import com.lhwdev.minecraft.railx.splitGraph.TravellingPointsForSplit;
//import com.simibubi.create.Create;
//import com.simibubi.create.content.trains.entity.TravellingPoint;
//import com.simibubi.create.content.trains.graph.DimensionPalette;
//import com.simibubi.create.content.trains.graph.TrackGraph;
//import net.minecraft.nbt.CompoundTag;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.ModifyVariable;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//
//@Mixin(TravellingPoint.class)
//public class TravellingPointMixin implements TravellingPointForSplit {
//	@Shadow public boolean blocked;
//
//	@Unique private TrackGraph railx$destinationGraph;
//
//	@Unique private boolean railx$enableSplitGraph;
//
//
//	@Override
//	public @Nullable TrackGraph railx$destinationGraph() {
//		return railx$destinationGraph;
//	}
//
//	@Override
//	public void railx$setDestinationGraph(@Nullable TrackGraph to) {
//		railx$destinationGraph = to;
//	}
//
//	@Override
//	public void railx$enableSplitGraph() {
//		railx$enableSplitGraph = true;
//	}
//
//
//	@ModifyVariable(method = "travel(Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
//		"DLcom/simibubi/create/content/trains/entity/TravellingPoint$ITrackSelector;" +
//		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IEdgePointListener;" +
//		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$ITurnListener;" +
//		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IPortalListener;)D",
//		at = @At("HEAD"), argsOnly = true, index = 1)
//	TrackGraph modifyGraphBeforeTravel(TrackGraph value) {
//		if(railx$destinationGraph != null) return railx$destinationGraph;
//		return value;
//	}
//
//	@Inject(method = "travel(Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
//		"DLcom/simibubi/create/content/trains/entity/TravellingPoint$ITrackSelector;" +
//		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IEdgePointListener;" +
//		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$ITurnListener;" +
//		"Lcom/simibubi/create/content/trains/entity/TravellingPoint$IPortalListener;)D",
//		at = @At("TAIL"), cancellable = true)
//	void afterTravel(
//		TrackGraph graph,
//		double distance,
//		TravellingPoint.ITrackSelector trackSelector,
//		TravellingPoint.IEdgePointListener signalListener,
//		TravellingPoint.ITurnListener turnListener,
//		TravellingPoint.IPortalListener portalListener,
//		CallbackInfoReturnable<Double> cir
//	) {
//		if(!railx$enableSplitGraph) return;
//		if(!blocked) return;
//		double moved = cir.getReturnValueD();
//		var result = TravellingPointsForSplit.INSTANCE.tryTravelThroughSplit(
//			(TravellingPoint) (Object) this,
//			distance - moved,
//			trackSelector,
//			signalListener,
//			turnListener,
//			portalListener
//		);
//		if(result != null) {
//			railx$destinationGraph = result.getOtherGraph();
//			cir.setReturnValue(moved + result.getMoved());
//		}
//	}
//
//
//	@ModifyVariable(method = "edgeTraversedFrom", at = @At("HEAD"), argsOnly = true, index = 1)
//	TrackGraph modifyGraphForEdgeTraversedFrom(TrackGraph value) {
//		if(railx$destinationGraph != null) return railx$destinationGraph;
//		return value;
//	}
//
//	@ModifyVariable(method = "reverse", at = @At("HEAD"), argsOnly = true, index = 1)
//	TrackGraph modifyGraphForReverse(TrackGraph value) {
//		if(railx$destinationGraph != null) return railx$destinationGraph;
//		return value;
//	}
//
//	@ModifyVariable(method = "getPositionWithOffset", at = @At("HEAD"), argsOnly = true, index = 1)
//	TrackGraph modifyGraphForGetPositionWithOffset(TrackGraph value) {
//		if(railx$destinationGraph != null) return railx$destinationGraph;
//		return value;
//	}
//
//	@Inject(method = "write", at = @At("RETURN"))
//	void onWrite(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
//		CompoundTag tag = cir.getReturnValue();
//		if(railx$destinationGraph != null) tag.putUUID("railx:DestinationGraph", railx$destinationGraph.id);
//	}
//
//	@Inject(method = "read", at = @At("RETURN"))
//	private static void onRead(
//		CompoundTag tag,
//		TrackGraph graph,
//		DimensionPalette dimensions,
//		CallbackInfoReturnable<TravellingPoint> cir
//	) {
//		if(tag.contains("railx:DestinationGraph")) {
//			var id = tag.getUUID("railx:DestinationGraph");
//			var destination = Create.RAILWAYS.trackNetworks.get(id);
//			if(destination == null) {
//				var sd = RailwayServerGlobals.INSTANCE.getSavedData();
//				if(sd != null) destination = sd.getTrackNetworks().get(id);
//			}
//			if(destination == null) {
//				System.err.println("?? destination == null");
//			}
//			((TravellingPointMixin) (Object) cir.getReturnValue()).railx$destinationGraph = destination;
//		}
//	}
//}
