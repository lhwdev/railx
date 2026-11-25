package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.utils.CompactBezierConnection
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState


class MiddleTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	FakeTrackBlockEntity(type, pos, state), MiddleTrackLikeBlockEntity {
	
	var connections: List<BezierConnection> = emptyList()
		private set
	
	override fun getConnectionValues(): List<BezierConnection> =
		connections
	
	private var pendingConnections: List<BezierConnection>? = null
	
	fun updateConnections(value: List<BezierConnection>) {
		check(value.all { it.primary }) { "given value is not primary" }
		val level = level
		if(level == null || level.getBlockEntity(blockPos) !== this) {
			pendingConnections = value
			return
		}
		
		val all = GlobalConnections[level].updateMiddle(this, value)
		if(all.isEmpty() && !level.isClientSide) {
			level.removeBlock(blockPos, false)
			return
		}
		
		connections = all
		notifyUpdate()
	}
	
	override fun onLoad() {
		super.onLoad()
		
		val pending = pendingConnections
		pendingConnections = null
		if((pending ?: connections).isEmpty() || RailXConfig.Server.middleTrack.removePrevious.isTrue) {
			level!!.removeBlock(blockPos, false)
			return
		}
		
		if(pending != null) updateConnections(pending)
	}
	
	// Be aware that this is also called on chunk unload!
	override fun setRemoved() {
		super.setRemoved()
		
		val level = level!!
		GlobalConnections[level].removeMiddle(this)
		connections = emptyList()
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


