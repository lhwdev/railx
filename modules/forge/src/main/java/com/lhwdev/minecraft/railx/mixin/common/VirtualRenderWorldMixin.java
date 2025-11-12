package com.lhwdev.minecraft.railx.mixin.common;

import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;


@Mixin(VirtualRenderWorld.class)
public abstract class VirtualRenderWorldMixin extends Level {
	protected VirtualRenderWorldMixin(
		WritableLevelData levelData,
		ResourceKey<Level> dimension,
		RegistryAccess registryAccess,
		Holder<DimensionType> dimensionTypeRegistration,
		Supplier<ProfilerFiller> profiler,
		boolean isClientSide,
		boolean isDebug,
		long biomeZoomSeed,
		int maxChainedNeighborUpdates
	) {
		super(
			levelData,
			dimension,
			registryAccess,
			dimensionTypeRegistration,
			profiler,
			isClientSide,
			isDebug,
			biomeZoomSeed,
			maxChainedNeighborUpdates
		);
	}
	
	@Override
	public void blockEntityChanged(@NotNull BlockPos pos) { /* no-op */ }
}
