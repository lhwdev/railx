package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.TravellingPointForSplit;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(CarriageBogey.class)
public class CarriageBogeyMixin {
	@Redirect(method = "<init>(Lcom/simibubi/create/content/trains/bogey/AbstractBogeyBlock;" +
		"ZLnet/minecraft/nbt/CompoundTag;)V", at = @At(value = "NEW", target = "()Lcom/simibubi/create/content" +
		"/trains/entity/TravellingPoint;"))
	private static TravellingPoint travellingPointForCtor() {
		return new TravellingPointForSplit();
	}
	
	@Redirect(method = "<init>(Lcom/simibubi/create/content/trains/bogey/AbstractBogeyBlock;" +
		"ZLnet/minecraft/nbt/CompoundTag;Lcom/simibubi/create/content/trains/entity/TravellingPoint;" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint;)V", at = @At(value = "INVOKE", target = "Lnet" +
		"/createmod/catnip/data/Couple;create(Ljava/lang/Object;Ljava/lang/Object;)" +
		"Lnet/createmod/catnip/data/Couple;", ordinal = 0))
	Couple<TravellingPoint> mapTravellingPointsForCtor(Object point, Object point2) {
		TravellingPointForSplit p, p2;
		if(point instanceof TravellingPointForSplit split) p = split;
		else p = new TravellingPointForSplit((TravellingPoint) point);
		
		if(point2 instanceof TravellingPointForSplit split) p2 = split;
		else p2 = new TravellingPointForSplit((TravellingPoint) point2);
		
		return Couple.create(p, p2);
	}
	
	@Redirect(method = "lambda$read$4", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/entity/TravellingPoint;read(Lnet/minecraft/nbt/CompoundTag;" +
		"Lcom/simibubi/create/content/trains/graph/TrackGraph;" +
		"Lcom/simibubi/create/content/trains/graph/DimensionPalette;)" +
		"Lcom/simibubi/create/content/trains/entity/TravellingPoint;"))
	private static TravellingPoint readTravellingPoint(CompoundTag tag, TrackGraph graph,
		DimensionPalette dimensions) {
		return TravellingPointForSplit.read(tag, graph, dimensions);
	}
}
