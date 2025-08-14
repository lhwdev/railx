package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(BezierConnection.class)
public class BezierConnectionMixin {
	@ModifyArg(method = "addItemsToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item" +
		"/ItemStack;<init>(Lnet/minecraft/world/level/ItemLike;I)V"), index = 0)
	private ItemLike getFlexiTrackRequirements(ItemLike item) {
		if(item instanceof TrackBlock) {
			var material = ((BezierConnection) (Object) this).getMaterial();
			if(material instanceof FlexiTrackMaterial flexi) return flexi.getFlexiBlock();
		}
		return item;
	}
}
