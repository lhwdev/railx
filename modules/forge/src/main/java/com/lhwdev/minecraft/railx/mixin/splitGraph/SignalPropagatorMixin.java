package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.PropagatingTrackGraph;
import com.lhwdev.minecraft.railx.splitGraph.TrackGraphConnectedIdUtils;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(SignalPropagator.class)
public class SignalPropagatorMixin {
	@ModifyVariable(method = {"onSignalRemoved", "notifySignalsOfNewNode", "propagateSignalGroup",
		"collectChainedSignals", "walkSignals(Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
		"Lcom/simibubi/create/content/trains/signal/SignalBoundary;ZLjava/util/function/Predicate;" +
		"Ljava/util/function/Predicate;Z)V"}, at = @At("HEAD"), argsOnly = true)
	private static TrackGraph graphForPropagator(TrackGraph value) {
		if(value instanceof PropagatingTrackGraph propagating) return propagating;
		return new PropagatingTrackGraph(Create.RAILWAYS, value);
	}
	
	@Definition(id = "train", local = @Local(type = Train.class))
	@Definition(id = "fGraph", field = "Lcom/simibubi/create/content/trains/entity/Train;" +
		"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;")
	@Definition(id = "graph", local = @Local(type = TrackGraph.class, argsOnly = true))
	@Expression("train.fGraph != graph")
	@ModifyExpressionValue(method = "notifyTrains", at = @At(value = "MIXINEXTRAS:EXPRESSION"), require = 0)
	private static boolean inDifferentGraphForNotify(
		boolean original,
		@Local(index = 7) Train train,
		@Local(index = 0, argsOnly = true) TrackGraph graph
	) {
		if(!original) return false;
		return !TrackGraphConnectedIdUtils.isReachableTo(train.graph, graph);
	}
}
