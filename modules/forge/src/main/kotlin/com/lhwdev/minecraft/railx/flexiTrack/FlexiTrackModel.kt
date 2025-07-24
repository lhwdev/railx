package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.Create
import com.tterrag.registrate.providers.DataGenContext
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ConfiguredModel


object FlexiTrackModel {
	fun registerBlockState(context: DataGenContext<Block, FlexiTrackBlock>, provider: BlockStateProvider) {
		provider.getVariantBuilder(context.entry).forAllStates {
			ConfiguredModel.builder()
				.modelFile(provider.models().getExistingFile(Create.asResource("block/track/x_ortho")))
				.build()
		}
	}
}
