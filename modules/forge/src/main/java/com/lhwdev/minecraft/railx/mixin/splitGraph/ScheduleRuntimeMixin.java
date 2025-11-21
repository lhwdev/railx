package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.AllConnectedTrackGraphs;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(ScheduleRuntime.class)
public class ScheduleRuntimeMixin {
	@Shadow public Train train;
	
	// not needed for ScheduleWaitCondition so far
	@WrapMethod(method = "startCurrentInstruction")
	DiscoveredPath startCurrentInstruction(Level level, Operation<DiscoveredPath> original) {
		var previous = train.graph;
		train.graph = new AllConnectedTrackGraphs(Create.RAILWAYS, previous);
		try {
			return original.call(level);
		} finally {
			train.graph = previous;
		}
	}
}
