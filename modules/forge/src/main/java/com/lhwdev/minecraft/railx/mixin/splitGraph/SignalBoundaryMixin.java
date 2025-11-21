package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.SlotObjects;
import com.lhwdev.minecraft.railx.splitGraph.SplitSignalPropagator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.createmod.catnip.data.Couple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.UUID;


@Mixin(SignalBoundary.class)
public class SignalBoundaryMixin {
	@Shadow private Couple<Map<UUID, Boolean>> chainedSignals;
	
	@Redirect(method = "resolveSignalChain", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/signal/SignalPropagator;collectChainedSignals(Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
		"Lcom/simibubi/create/content/trains/signal/SignalBoundary;Z)Ljava/util/Map;"))
	Map<UUID, Boolean> collectChainedSignals(TrackGraph graph, SignalBoundary signal, boolean front) {
		return SplitSignalPropagator.INSTANCE.collectChainedSignals(graph, signal, front);
	}
	
	@WrapOperation(method = "resolveSignalChain", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/graph/TrackGraph;getPoint(Lcom/simibubi/create/content/trains/graph/EdgePointType;Ljava/util/UUID;)" +
		"Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;"))
	TrackEdgePoint getPointForResolveSignalChain(
		TrackGraph instance,
		EdgePointType<?> type,
		UUID id,
		Operation<TrackEdgePoint> original,
		@Local(index = 2, argsOnly = true) boolean side
	) {
		var signals = chainedSignals.get(side);
		if(signals instanceof SlotObjects.ChainedSignals chained) {
			var graph = chained.graphs.get(id);
			if(graph != null) return original.call(graph, type, id);
		}
		return original.call(instance, type, id);
	}
}
