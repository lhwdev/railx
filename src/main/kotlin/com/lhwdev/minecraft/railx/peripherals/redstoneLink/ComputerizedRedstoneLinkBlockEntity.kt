package com.lhwdev.minecraft.railx.peripherals.redstoneLink

import com.lhwdev.minecraft.railx.Registries
import com.lhwdev.minecraft.railx.api.blockEntity.PeripheralBehavior
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional


class ComputerizedRedstoneLinkBlockEntity(pos: BlockPos, state: BlockState) :
	SmartBlockEntity(Registries.COMPUTERIZED_REDSTONE_LINK_BLOCK_ENTITY.get(), pos, state) {
	
	lateinit var behavior: ComputerizedRedstoneLinkBehavior
		private set
	
	private val peripheralBehavior = object : PeripheralBehavior<ComputerizedRedstoneLinkPeripheral>() {
		override fun createPeripheral() =
			ComputerizedRedstoneLinkPeripheral(behavior)
		
		override fun willProvidePeripheralToSide(side: Direction) = side == Direction.DOWN
	}
	
	override fun tick() {
		super.tick()
	}
	
	override fun addBehaviours(list: MutableList<BlockEntityBehaviour>) {
		behavior = ComputerizedRedstoneLinkBehavior(this)
		list += behavior
	}
	
	override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> =
		peripheralBehavior.getCapability(cap, side) ?: super.getCapability(cap, side)
}
