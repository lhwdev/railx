package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.TrainMigrationForSplit;
import com.simibubi.create.content.trains.entity.TrainMigration;
import com.simibubi.create.content.trains.graph.*;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(TrainMigration.class)
public class TrainMigrationMixin implements TrainMigrationForSplit {
	@Shadow Couple<TrackNodeLocation> locations;
	@Shadow double positionOnOldEdge;
	
	@Unique
	int railx$graphIndex;
	
	
	@Override
	public int railx$getGraphIndex() {
		return railx$graphIndex;
	}
	
	@Override
	public void railx$setGraphIndex(int index) {
		railx$graphIndex = index;
	}
	
	@Override
	public @Nullable TrackGraphLocation railx$tryMigratingNodesTo(@NotNull TrackGraph graph) {
		TrackNode node1 = graph.locateNode(locations.getFirst());
		TrackNode node2 = graph.locateNode(locations.getSecond());
		if(node1 != null && node2 != null) {
			TrackEdge edge = graph.getConnectionsFrom(node1).get(node2);
			if(edge != null) {
				TrackGraphLocation graphLocation = new TrackGraphLocation();
				graphLocation.graph = graph;
				graphLocation.edge = locations;
				graphLocation.position = positionOnOldEdge;
				return graphLocation;
			}
		}
		
		return null;
	}
	
	@Inject(method = "write", at = @At("RETURN"))
	void onWrite(CallbackInfoReturnable<CompoundTag> cir) {
		if(railx$graphIndex != 0)
			cir.getReturnValue().putInt("railx:GraphIndex", railx$graphIndex);
	}
	
	@Inject(method = "read", at = @At("RETURN"))
	private static void onRead(
		CompoundTag tag,
		DimensionPalette dimensions,
		CallbackInfoReturnable<TrainMigration> cir
	) {
		if(tag.contains("railx:GraphIndex"))
			((TrainMigrationMixin) (Object) cir.getReturnValue()).railx$graphIndex = tag.getInt("railx:GraphIndex");
	}
}
