package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.lhwdev.minecraft.railx.flexiTrack.mixin.GameDataMixinHelper;
import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.GameData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@SuppressWarnings("UnstableApiUsage")
@Mixin(GameData.class)
public class GameDataMixin {
	@Inject(method = "getBlockStateIDMap", at = @At("RETURN"), cancellable = true, remap = false)
	private static void onGetBlockStateIDMap(CallbackInfoReturnable<IdMapper<BlockState>> cir) {
		cir.setReturnValue(GameDataMixinHelper.INSTANCE.mapBlockStateIDMap(cir.getReturnValue()));
	}
}
