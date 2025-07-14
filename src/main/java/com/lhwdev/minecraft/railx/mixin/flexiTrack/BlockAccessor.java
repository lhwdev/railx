package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(Block.class)
public interface BlockAccessor {
	@Accessor
	void setStateDefinition(StateDefinition<Block, BlockState> value);
}
