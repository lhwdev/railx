package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.TrackGraphForSplit;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphVisualizer;
import net.createmod.catnip.theme.Color;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;
import java.util.Random;


@Mixin(value = TrackGraphVisualizer.class, priority = 10000)
public class TrackGraphVisualizerMixin {
	@Redirect(method = "debugViewGraph", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains" +
		"/graph/TrackGraph;color:Lnet/createmod/catnip/theme/Color;", ordinal = 2, opcode = Opcodes.GETFIELD, remap =
		false), remap = false, require = 0)
	private static Color getGraphColor(TrackGraph instance) {
		var id = ((TrackGraphForSplit) instance).railx$getConnectedId();
		var graph = CreateClient.RAILWAYS.trackNetworks.get(id);
		return graph != null ? graph.color : id == null ? new Color(0xff000000) : new Color(
			new Random(Objects.hashCode(id)).nextInt(), false);
	}
	
	@Redirect(method = "debugViewGraph", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains" +
		"/graph/TrackGraph;color:Lnet/createmod/catnip/theme/Color;", ordinal = 4, opcode = Opcodes.GETFIELD, remap =
		false), remap = false, require = 0)
	private static Color getGraphColorForCurve(TrackGraph instance) {
		var id = ((TrackGraphForSplit) instance).railx$getConnectedId();
		var graph = CreateClient.RAILWAYS.trackNetworks.get(id);
		return graph != null ? graph.color : id == null ? new Color(0xff000000) : new Color(
			new Random(Objects.hashCode(id)).nextInt(), false);
	}
}
