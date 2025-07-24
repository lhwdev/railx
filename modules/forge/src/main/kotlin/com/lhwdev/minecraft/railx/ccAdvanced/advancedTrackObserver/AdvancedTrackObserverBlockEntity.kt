package com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver

import com.simibubi.create.content.trains.observer.TrackObserverBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState


class AdvancedTrackObserverBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	TrackObserverBlockEntity(type, pos, state) {
	
	val rule = AdvancedRule()
	
	val advancedObserver get() = observer as? AdvancedTrackObserver
	
	
	fun onRuleUpdated() {
		advancedObserver?.let { it.rule = rule }
	}
	
	
	override fun read(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		super.read(tag, registries, clientPacket)
		
		rule.read(tag.getCompound("Rule"))
	}
	
	override fun write(tag: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
		super.write(tag, registries, clientPacket)
		
		tag.put("Rule", rule.write())
	}
}
