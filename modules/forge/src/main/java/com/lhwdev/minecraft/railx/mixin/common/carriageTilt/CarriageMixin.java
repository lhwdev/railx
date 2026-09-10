package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.carriageTilt.DimensionalCarriageEntityWithTilt;
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection;
import com.lhwdev.minecraft.railx.flexiTrack.rotate.RotationUtils;
import com.lhwdev.minecraft.railx.splitGraph.TravelingPointSplitUtils;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Carriage.class)
public abstract class CarriageMixin {
	@Shadow
	public abstract TravellingPoint getLeadingPoint();
	
	@Shadow
	public abstract TravellingPoint getTrailingPoint();
	
	@Shadow
	public Train train;
	
	@Shadow
	public abstract Carriage.DimensionalCarriageEntity getDimensional(ResourceKey<Level> dimension);
	
	
	@Inject(method = "updateContraptionAnchors", at = @At("TAIL"))
	void onUpdateContraptionAnchors(CallbackInfo ci) {
		if(RailXConfig.Server.Value.getCommon().getCarriageTilt().isFalse()) {
			return;
		}
		
		// all points are not null
		for(boolean leading : Iterate.trueAndFalse) {
			TravellingPoint point = leading ? getLeadingPoint() : getTrailingPoint();
			TravellingPoint otherPoint = !leading ? getLeadingPoint() : getTrailingPoint();
			ResourceKey<Level> dimension = point.node1.getLocation().dimension;
			ResourceKey<Level> otherDimension = otherPoint.node1.getLocation().dimension;
			
			if(dimension.equals(otherDimension) && leading) continue;
			
			var dce = (DimensionalCarriageEntityWithTilt) getDimensional(dimension);
			Couple<Double> tilt = dce.getRailx$tilt();
			
			double RAD_TO_DEG = 180 / Math.PI;
			
			TrackGraph graph = TravelingPointSplitUtils.getDestinationGraph(point);
			if(graph == null) graph = train.graph;
			FlexiDirection direction = RotationUtils.getDirection(point, graph);
			tilt.setFirst(RotationUtils.getCant(direction) * RAD_TO_DEG);
			
			graph = TravelingPointSplitUtils.getDestinationGraph(otherPoint);
			if(graph == null) graph = train.graph;
			direction = RotationUtils.getDirection(otherPoint, graph);
			tilt.setSecond(RotationUtils.getCant(direction) * RAD_TO_DEG);
		}
	}
}
