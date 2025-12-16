package com.lhwdev.minecraft.railx.mixin.compat.railways.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.railwayteam.railways.util.CustomTrackOverlayRendering;
import com.simibubi.create.content.trains.track.TrackShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;


@Mixin(CustomTrackOverlayRendering.class)
public class CustomTrackOverlayRenderingMixin {
	@WrapOperation(method = "prepareTrackOverlay", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/track/TrackShape;getAxes()Ljava/util/List;", remap = false), remap = false)
	private static List<Vec3> getTrackShapeAxes(
		TrackShape instance,
		Operation<List<Vec3>> original,
		@Local(index = 0, argsOnly = true) BlockGetter world,
		@Local(index = 1, argsOnly = true) BlockPos pos,
		@Local(index = 2, argsOnly = true) BlockState state
	) {
		if(!(state.getBlock() instanceof FlexiTrackBlock track)) return original.call(instance);
		return track.flexiShape(world, pos).getTangents();
	}
}
