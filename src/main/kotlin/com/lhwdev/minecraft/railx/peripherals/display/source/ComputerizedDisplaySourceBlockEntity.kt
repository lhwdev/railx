package com.lhwdev.minecraft.railx.peripherals.display.source

import com.lhwdev.minecraft.railx.Registries
import com.lhwdev.minecraft.railx.api.blockEntity.PeripheralBehavior
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.*

class ComputerizedDisplaySourceBlockEntity(pos: BlockPos, state: BlockState) :
	BlockEntity(
		Registries.COMPUTERIZED_DISPLAY_SOURCE_BLOCK_ENTITY.get(),
		pos,
		state
	) {
	private val peripheralBehavior = object : PeripheralBehavior<ComputerizedDisplaySourcePeripheral>() {
		override fun createPeripheral() =
			ComputerizedDisplaySourcePeripheral(this@ComputerizedDisplaySourceBlockEntity)
		
		override fun willProvidePeripheralToSide(side: Direction) =
			side == blockState.getValue(ComputerizedDisplaySourceBlock.FACING)
	}
	
	val displayLinks = EnumMap<Direction, DisplayData>(net.minecraft.core.Direction::class.java)
	
	fun getFromPos(pos: BlockPos): DisplayData? {
		// @todo: optimize lol
		for(d in Direction.entries) {
			if(blockPos.relative(d) == pos) {
				return displayLinks[d]
			}
		}
		onNeighborChange(null, level!!, blockPos, null)
		return getFromPos(pos)
	}
	
	class DisplayData(var blockEntity: DisplayLinkBlockEntity) {
		var toDisplay = listOf<MutableComponent>()
	}
	
	fun onNeighborChange(state: BlockState?, level: LevelReader, pos: BlockPos, neighbor: BlockPos?) {
		for(dir in Direction.entries) {
			val location = pos.relative(dir)
			val blockState = level.getBlockState(location)
			if(blockState.`is`(AllBlocks.DISPLAY_LINK.get())) {
				displayLinks[dir] = DisplayData(level.getBlockEntity(location) as DisplayLinkBlockEntity)
				addLink(dir)
			} else {
				if(displayLinks.containsKey(dir)) {
					displayLinks.remove(dir)
					deleteLink(dir)
				}
			}
		}
	}
	
	fun onBlockBreak() {
		if(peripheralBehavior.lazyPeripheral.isPresent) {
			for(dir in Direction.entries) {
				if(displayLinks.containsKey(dir)) {
					peripheralBehavior.peripheral.deleteLink(dir)
				} else {
					peripheralBehavior.peripheral.closeHandle(dir)
				}
			}
		}
	}
	
	fun addLink(dir: Direction) {
		if(peripheralBehavior.lazyPeripheral.isPresent) peripheralBehavior.peripheral.addLink(dir)
	}
	
	fun deleteLink(dir: Direction) {
		if(peripheralBehavior.lazyPeripheral.isPresent) peripheralBehavior.peripheral.deleteLink(dir)
	}
}
