package com.lhwdev.minecraft.railx.mixin.compat.railways.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock;
import com.railwayteam.railways.registry.CREdgePointTypes;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;


@Mixin(TrackTargetingBlockItem.class)
public class TrackTargetingBlockItemMixin {
	// Want to remove inject into MixinTrackTargetingBlockItem.checkGraphLocation, but we cannot inject into Mixin
	// class itself.
	// TODO: find better ways?
	@Inject(method = "withGraphLocation", at = @At("HEAD"), remap = false, cancellable = true)
	private static void withGraphLocation(
		Level level,
		BlockPos pos,
		boolean front,
		BezierTrackPointLocation targetBezier,
		EdgePointType<?> type,
		BiConsumer<TrackTargetingBlockItem.OverlapResult, TrackGraphLocation> callback,
		CallbackInfo ci
	) {
		if(type != CREdgePointTypes.COUPLER && type != CREdgePointTypes.SWITCH)
			return;
		
		var state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof FlexiTrackBlock track))
			return;
		
		ci.cancel();
		TrackTargetingBlockItem.OverlapResult NOT_STRAIGHT = null;
		try {
			NOT_STRAIGHT = TrackTargetingBlockItem.OverlapResult.valueOf("NOT_STRAIGHT");
		} catch(IllegalArgumentException ignored) {
		}
		
		if(targetBezier != null) {
			callback.accept(
				NOT_STRAIGHT == null ? TrackTargetingBlockItem.OverlapResult.NO_TRACK : NOT_STRAIGHT,
				null
			);
			return;
		}
		var shape = track.flexiShape(level, pos);
		if(shape.getAxesCount() != 1) {
			callback.accept(TrackTargetingBlockItem.OverlapResult.JUNCTION, null);
			return;
		}
		
		var targetDirection = front ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
		var location = TrackGraphHelper.getGraphLocationAt(level, pos, targetDirection, shape.getAxis1().getTangent());
		
		if(location == null) {
			callback.accept(TrackTargetingBlockItem.OverlapResult.NO_TRACK, null);
			return;
		}
		
		Couple<TrackNode> nodes = location.edge.map(location.graph::locateNode);
		TrackEdge edge = location.graph.getConnection(nodes);
		if(edge == null)
			return;
		
		EdgeData edgeData = edge.getEdgeData();
		double edgePosition = location.position;
		
		for(TrackEdgePoint edgePoint : edgeData.getPoints()) {
			double otherEdgePosition = edgePoint.getLocationOn(edge);
			double distance = Math.abs(edgePosition - otherEdgePosition);
			if(distance > .75)
				continue;
			if(edgePoint.canCoexistWith(type, front) && distance < .25)
				continue;
			
			callback.accept(TrackTargetingBlockItem.OverlapResult.OCCUPIED, location);
			return;
		}
		
		callback.accept(TrackTargetingBlockItem.OverlapResult.VALID, location);
	}
}
