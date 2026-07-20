package com.lhwdev.minecraft.railx.advancedRoller

import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

class AdvancedRollerBlock(properties: Properties) : RollerBlock(properties) {
	@Suppress("UNCHECKED_CAST")
	override fun getBlockEntityClass(): Class<RollerBlockEntity> =
		AdvancedRollerBlockEntity::class.java as Class<RollerBlockEntity>
	
	override fun getBlockEntityType(): BlockEntityType<out RollerBlockEntity> =
		AllBlockEntityTypes.AdvancedMechanicalRoller.get()
}
