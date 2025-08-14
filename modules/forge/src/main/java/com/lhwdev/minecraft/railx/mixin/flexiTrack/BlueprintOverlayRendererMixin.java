package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(BlueprintOverlayRenderer.class)
public class BlueprintOverlayRendererMixin {
	@ModifyArg(method = "displayTrackRequirements", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item" +
		"/ItemStack;<init>(Lnet/minecraft/world/level/ItemLike;I)V"), index = 0)
	private static ItemLike getFlexiTrackRequirements(
		ItemLike item,
		@Local(argsOnly = true, ordinal = 0) TrackPlacement.PlacementInfo info
	) {
		if(item instanceof TrackBlock) {
			var material = info.trackMaterial;
			if(material instanceof FlexiTrackMaterial flexi) return flexi.getFlexiBlock();
		}
		return item;
	}
}
