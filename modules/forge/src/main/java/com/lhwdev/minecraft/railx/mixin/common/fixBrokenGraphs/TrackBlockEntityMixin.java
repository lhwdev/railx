package com.lhwdev.minecraft.railx.mixin.common.fixBrokenGraphs;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.commands.FixTrackBlockKt;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin extends SmartBlockEntity {
	public TrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {super(type, pos, state);}
	
	@Inject(method = "initialize", at = @At("RETURN"), remap = false)
	void onInitialize(CallbackInfo ci) {
		if(!RailXConfig.Server.Value.getCommon().getFixBrokenGraphs().get())
			return;
		
		Level level = this.level;
		if(level == null) return;
		
		if(!level.isClientSide)
			FixTrackBlockKt.fixTrackBlock(level, getBlockPos());
	}
}
