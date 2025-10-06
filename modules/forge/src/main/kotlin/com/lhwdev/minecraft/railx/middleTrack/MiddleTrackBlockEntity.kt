package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.utils.CompactBezierConnection
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState


class MiddleTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	FakeTrackBlockEntity(type, pos, state) {
	
	var connections: List<BezierConnection> = emptyList()
		private set
	
	
	private var pendingConnections: List<BezierConnection>? = null
	
	fun updateConnections(value: List<BezierConnection>) {
		val level = level
		if(level == null || level.getBlockEntity(blockPos) !is MiddleTrackBlockEntity) {
			pendingConnections = value
			return
		}
		
		if(value.isEmpty() && !level.isClientSide) {
			connections = emptyList()
			level.removeBlock(blockPos, false)
			return
		}
		
		notifyUpdate()
		
		if(level.isClientSide) {
			GlobalConnections[level].updateMiddle(this, value)
		}
		connections = value
	}
	
	override fun setLevel(level: Level) {
		super.setLevel(level)
		
		// pendingConnections?.let {
		// 	pendingConnections = null
		// 	updateConnections(it)
		// }
	}
	
	override fun onLoad() {
		super.onLoad()
		
		pendingConnections?.let {
			pendingConnections = null
			updateConnections(it)
		}
	}
	
	// Be aware that this is also called on chunk unload!
	override fun setRemoved() {
		super.setRemoved()
		
		val level = level
		if(level != null) {
			GlobalConnections[level].removeMiddle(this)
		}
	}
	
	
	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.put("Connections", connections.mapTo(ListTag()) { CompactBezierConnection.write(it, blockPos) })
	}
	
	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		updateConnections((tag.get("Connections") as ListTag).mapNotNull { t ->
			CompactBezierConnection.read(t as CompoundTag, blockPos)
				?.also { require(it.primary) { "curve is not primary" } }
		})
	}
}


