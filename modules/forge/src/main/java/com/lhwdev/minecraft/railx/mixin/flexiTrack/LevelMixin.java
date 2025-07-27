package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.lhwdev.minecraft.railx.flexiTrack.FlexiBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(Level.class)
public class LevelMixin {
	@ModifyVariable(
		method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
		at = @At("HEAD"),
		index = 2,
		argsOnly = true
	)
	BlockState onSetBlock(BlockState state, BlockPos pos) {
		Level level = (Level) (Object) this;
//		if(state instanceof FlexiBlockState flexiState) {
//			return flexiState.setStateOnLevel(level, pos);
//		} else {
			return state;
//		}
	}
}
