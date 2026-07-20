package com.lhwdev.minecraft.railx.advancedRoller

import com.lhwdev.minecraft.railx.common.gravelLayer.GravelLayerBlock
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class AdvancedRollerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	RollerBlockEntity(type, pos, state) {
	
	override fun isValidMaterial(newFilter: ItemStack): Boolean {
		val item = newFilter.item
		if(item is BlockItem) {
			val block = item.block
			if(block is GravelLayerBlock) return true
		}
		
		return super.isValidMaterial(newFilter)
	}
}
