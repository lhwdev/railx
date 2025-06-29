package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.content.trains.track.TrackBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class FlexiTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	TrackBlockEntity(type, pos, state) {
	
	inner class State : FlexiState(FlexiShape.Single(FlexiDirection.Known.Divisions[0])) {
	
	}
	
	val state: FlexiState = State()
	
	override fun write(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		super.write(tag, registries, clientPacket)
		tag.put("FlexiState", state.write())
	}
}
