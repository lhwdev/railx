package com.lhwdev.minecraft.railx.mixin;


import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = Carriage.class, remap = false)
public class DerailedTrainHotfix {
	@Inject(method = "read", at = @At("RETURN"))
	private static void onRead(
		CompoundTag tag,
		HolderLookup.Provider registries,
		TrackGraph graph,
		DimensionPalette dimensions,
		CallbackInfoReturnable<Carriage> cir,
		@Local Carriage self
	) {
		tag.getList("EntityPositioning", Tag.TAG_COMPOUND).forEach(t -> {
			var level = dimensions.decode(((CompoundTag) t).getInt("Dim"));
			railx$fixProblem(self, level);
		});
	}
	
	@Inject(method = "updateContraptionAnchors", at = @At("RETURN"))
	void onUpdateContraptionAnchors(CallbackInfo ci) {
		var self = (Carriage) (Object) this;
		railx$fixProblem(self, self.leadingBogey().getDimension());
	}
	
	@Unique
	private static void railx$fixProblem(Carriage self, ResourceKey<Level> level) {
		if(level == null) return;
		var dim = self.getDimensional(level);
		Vec3 best;
		if(self.train != null) {
			best = self.train.carriages.stream().map(c -> c.getDimensional(level).positionAnchor)
				.filter(c -> c != null && Double.isFinite(c.x)).findFirst().orElse(new Vec3(0, 0, 0));
		} else {
			best = dim.positionAnchor != null && Double.isFinite(dim.positionAnchor.x) ? dim.positionAnchor :
				dim.rotationAnchors != null && dim.rotationAnchors.getFirst() != null && Double.isFinite(dim.rotationAnchors.getFirst().x) ?
					dim.rotationAnchors.getFirst() : new Vec3(97, 78, 73);
		}
		if(dim.positionAnchor != null && Double.isNaN(dim.positionAnchor.x)) {
			dim.positionAnchor = best;
		}
		if(dim.rotationAnchors != null && dim.rotationAnchors.getFirst() != null && Double.isNaN(dim.rotationAnchors.getFirst().x)) {
			dim.rotationAnchors = Couple.create(best, best);
		}
	}
}
