package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.realisticSpeed.BlockPropertiesWithSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(BlockBehaviour.Properties.class)
public class PropertiesMixin implements BlockPropertiesWithSource {
	@Unique
	private Block railx$initialPropertiesSource;
	
	
	@Override
	public @Nullable Block getInitialPropertiesSource() {
		return railx$initialPropertiesSource;
	}
	
	@Override
	public void setInitialPropertiesSource(@Nullable Block block) {
		railx$initialPropertiesSource = block;
	}
}
