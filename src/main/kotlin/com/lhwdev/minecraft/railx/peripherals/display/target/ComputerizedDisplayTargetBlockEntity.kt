package com.lhwdev.minecraft.railx.peripherals.display.target

import com.lhwdev.minecraft.railx.Registries
import com.lhwdev.minecraft.railx.api.blockEntity.PeripheralBehavior
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ComputerizedDisplayTargetBlockEntity(pos: BlockPos, state: BlockState) :
	BlockEntity(
		Registries.COMPUTERIZED_DISPLAY_TARGET_BLOCK_ENTITY.get(),
		pos,
		state
	) {
	
	private val peripheralBehavior = object : PeripheralBehavior<ComputerizedDisplayTargetPeripheral>() {
		override fun createPeripheral() =
			ComputerizedDisplayTargetPeripheral(this@ComputerizedDisplayTargetBlockEntity)
		
		override fun willProvidePeripheralToSide(side: Direction) =
			side == blockState.getValue(ComputerizedDisplayTargetBlock.FACING)
	}
	
	
	var maxHeight = 4
	var maxWidth = 15
	
	fun acceptText(line: Int, list: List<MutableComponent>, displayLinkContext: DisplayLinkContext) {
		if(peripheralBehavior.lazyPeripheral.isPresent) {
			peripheralBehavior.peripheral.acceptText(line, list, displayLinkContext)
		}
	}
}
