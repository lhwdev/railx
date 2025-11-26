package com.lhwdev.minecraft.railx.registry

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity
import com.simibubi.create.foundation.utility.AdventureUtil
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer


abstract class BlockEntityConfigurationPacket<BE : SyncedBlockEntity>(protected val pos: BlockPos) :
	ServerboundPacketBase() {
	protected open val maxRange: Double get() = 30.0
	
	protected open val causeUpdate: Boolean get() = true
	
	
	protected open fun applySettings(be: BE) {}
	
	protected open fun applySettings(player: ServerPlayer, be: BE) {
		applySettings(be)
	}
	
	
	final override fun handle(player: ServerPlayer?) {
		if(player == null || player.isSpectator || AdventureUtil.isAdventure(player))
			return
		
		val world = player.level()
		if(world == null || !world.isLoaded(pos)) return
		if(!pos.closerThan(player.blockPosition(), maxRange)) return
		
		val blockEntity = world.getBlockEntity(pos) as? SyncedBlockEntity ?: return
		@Suppress("UNCHECKED_CAST")
		applySettings(player, blockEntity as BE)
		
		if(causeUpdate) {
			blockEntity.sendData()
			blockEntity.setChanged()
		}
	}
}

