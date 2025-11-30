package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.TrackGraphForSplitUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.TrackGraph;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;


@Mixin(EdgeData.class)
public class EdgeDataMixin {
	@ModifyExpressionValue(method = "getEffectiveEdgeGroupId", at = @At(value = "FIELD", target = "Lcom/simibubi" +
		"/create/content/trains/graph/TrackGraph;id:Ljava/util/UUID;", opcode = Opcodes.GETFIELD, remap = false), remap = false)
	UUID getGraphPassiveId(UUID original, @Local(argsOnly = true, index = 1) TrackGraph graph) {
		var id = TrackGraphForSplitUtils.getConnectedId(graph);
		if(id != null) return id;
		return original;
	}
}
