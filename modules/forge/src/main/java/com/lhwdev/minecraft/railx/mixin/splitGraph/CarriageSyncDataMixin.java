package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.CarriageSyncDataForSplit;
import com.simibubi.create.content.trains.entity.CarriageSyncData;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(CarriageSyncData.class)
public class CarriageSyncDataMixin implements CarriageSyncDataForSplit {
	@Unique public TrackGraph railx$connectedGraph;
	
	@Override
	public void railx$setConnectedGraph(@Nullable TrackGraph graph) {
		railx$connectedGraph = graph;
	}
	
	@Redirect(method = {"apply", "approach"}, at = @At(value = "FIELD", target = "Lcom/simibubi/create/content" +
		"/trains/entity/Train;graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;", opcode = Opcodes.GETFIELD, remap = false), remap = false)
	TrackGraph getGraphForApproach(Train train) {
		return railx$connectedGraph;
	}
}
