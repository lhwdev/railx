package com.lhwdev.minecraft.railx.mixin.common.reservedSignal;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.ReservedSignalNixieTubeRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(NixieTubeRenderer.class)
public class NixieTubeRendererMixin {
	@Redirect(method = "renderAsSignal", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render" +
		"/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;" +
		"Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"))
	SuperByteBuffer partial(
		PartialModel partial,
		BlockState referenceState,
		@Local(index = 1, argsOnly = true) NixieTubeBlockEntity be
	) {
		if(RailXConfig.Server.Value.getCommon().getReservedSignal().isFalse())
			return CachedBuffers.partial(partial, referenceState);
		var result = ReservedSignalNixieTubeRenderer.INSTANCE.partialModelBase(be, partial, referenceState);
		return result != null ? result : CachedBuffers.partial(partial, referenceState);
	}
}
