package com.lhwdev.minecraft.railx.realisticSpeed

import com.lhwdev.minecraft.railx.registry.AllCustoms
import net.minecraftforge.fml.ModList


internal object BlockMaterialResolvers {
	fun register() {
		DatapackMaterialResolver.register()
		
		val registry = AllCustoms.Registry
		registry.simple("data", AllCustoms.BlockMaterials) { DatapackMaterialResolver }
		if(ModList.get().isLoaded("framedblocks"))
			registry.simple("framed_block", AllCustoms.BlockMaterials) { FramedBlockMaterialResolver }
		if(ModList.get().isLoaded("copycats"))
			registry.simple("copycat", AllCustoms.BlockMaterials) { CopycatBlockMaterialResolver }
		registry.simple("default", AllCustoms.BlockMaterials) { DefaultMaterialResolver }
	}
}
