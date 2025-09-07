package com.lhwdev.minecraft.railx.mixin.middleTrack;

import com.lhwdev.minecraft.railx.middleTrack.BezierConnectionUtils;
import com.lhwdev.minecraft.railx.registry.AllBlocks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;


@Mixin(TrackBlockEntity.class)
public class TrackBlockEntityMixin {
	@Shadow
	protected Level level;
	
	/**
	 * @author lhwdev
	 * @reason too lazy to inject
	 */
	@Overwrite
	public void manageFakeTracksAlong(BezierConnection bc, boolean remove) {
		List<BlockPos> blocks = BezierConnectionUtils.INSTANCE.rasterizeOrdered(bc);
		
		int index = 0;
		for(BlockPos pos : blocks) {
			pos = pos.offset(bc.bePositions.getFirst())
				.above(1);
			
			BlockState stateAtPos = level.getBlockState(pos);
			boolean present = com.simibubi.create.AllBlocks.FAKE_TRACK.has(stateAtPos) ||
				AllBlocks.INSTANCE.getMiddleTrack().has(stateAtPos);
			
			if(remove) {
				if(present)
					level.removeBlock(pos, false);
				continue;
			}
			
			FluidState fluidState = stateAtPos.getFluidState();
			if(!fluidState.isEmpty() && !fluidState.isSourceOfType(Fluids.WATER))
				continue;
			
			if(!present && stateAtPos.canBeReplaced())
				level.setBlock(
					pos,
					ProperWaterloggedBlock.withWater(
						level,
						com.simibubi.create.AllBlocks.FAKE_TRACK.getDefaultState(),
						pos
					), 3
				);
			FakeTrackBlock.keepAlive(level, pos);
			index++;
		}
	}
}
