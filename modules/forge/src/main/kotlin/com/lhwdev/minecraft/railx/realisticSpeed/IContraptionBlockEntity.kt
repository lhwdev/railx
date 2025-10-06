package com.lhwdev.minecraft.railx.realisticSpeed

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate


interface IContraptionBlockEntity {
	var requiresBlockEntity: Boolean
	
	fun onReadBlockEntity(level: Level, info: StructureTemplate.StructureBlockInfo, tag: CompoundTag): BlockEntity?
}
