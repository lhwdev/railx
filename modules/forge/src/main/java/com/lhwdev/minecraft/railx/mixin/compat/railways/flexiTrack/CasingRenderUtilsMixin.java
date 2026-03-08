package com.lhwdev.minecraft.railx.mixin.compat.railways.flexiTrack;

import com.lhwdev.minecraft.railx.compat.railways.RailwaysCasingExtension;
import com.lhwdev.minecraft.railx.compat.railways.flexiTrack.FlexiTrackCasingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CasingRenderUtils.class)
public class CasingRenderUtilsMixin {
	@Inject(method = "renderBezierCasings", at = @At("HEAD"), cancellable = true, remap = false)
	private static void renderTallBezierCasings(
		PoseStack ms,
		Level level,
		PartialModel texturedPartial,
		BlockState state,
		VertexConsumer vb,
		BezierConnection bc,
		CallbackInfo ci
	) {
		int heightDiff = Math.abs(bc.bePositions.get(false).getY() - bc.bePositions.get(true).getY());
		if(heightDiff == 0 && !RailwaysCasingExtension.isAlternate(bc)) {
			FlexiTrackCasingRenderer.INSTANCE.renderTallBezierCasingsOld(ms, level, state, vb, bc);
			ci.cancel();
		}
	}
}
