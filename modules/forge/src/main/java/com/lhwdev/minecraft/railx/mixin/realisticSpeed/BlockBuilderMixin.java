package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.realisticSpeed.BlockPropertiesWithSource;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BlockBuilder.class)
class BlockBuilderMixin<T extends Block, P> {
	@Unique
	private NonNullSupplier<? extends Block> railx$initialPropertiesSource;
	
	
	@Inject(method = "initialProperties", at = @At("RETURN"))
	void onInitialProperties(NonNullSupplier<? extends Block> block, CallbackInfoReturnable<BlockBuilder<T, P>> cir) {
		railx$initialPropertiesSource = block;
	}
	
	@Inject(method = "createEntry()Lnet/minecraft/world/level/block/Block;", at = @At("RETURN"))
	void onCreateEntry(CallbackInfoReturnable<T> cir) {
		if(railx$initialPropertiesSource != null) {
			((BlockPropertiesWithSource) cir.getReturnValue().properties())
				.setInitialPropertiesSource(railx$initialPropertiesSource.get());
		}
	}
}
