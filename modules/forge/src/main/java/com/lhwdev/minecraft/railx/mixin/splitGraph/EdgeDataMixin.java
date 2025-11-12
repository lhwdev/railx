package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.TrackGraphForSplitUtils;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.TrackGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;


@Mixin(EdgeData.class)
public class EdgeDataMixin {
	@Redirect(method = "getEffectiveEdgeGroupId", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content" +
		"/trains/graph/TrackGraph;id:Ljava/util/UUID;"))
	UUID getGraphPassiveId(TrackGraph graph) {
		var id = TrackGraphForSplitUtils.getConnectedId(graph);
		if(id != null) return id;
		return graph.id;
	}
}
