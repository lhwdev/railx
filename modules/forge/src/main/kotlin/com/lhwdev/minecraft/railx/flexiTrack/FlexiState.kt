package com.lhwdev.minecraft.railx.flexiTrack

import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.phys.Vec3


data class FlexiState(
	val shape: FlexiShape = FlexiShape.Empty,
	// val offset: Vec3 = Vec3.ZERO,
) {
	companion object {
		val Base = FlexiState()
		
		fun read(tag: CompoundTag): FlexiState = FlexiState(
			shape = FlexiShape.read(tag.getCompound("Shape")),
		)
	}
	
	fun write(): CompoundTag = CompoundTag().also { tag ->
		tag.put("Shape", shape.write())
	}
}
