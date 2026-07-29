package com.lhwdev.minecraft.railx.common

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.AABB


interface ScrollValueBehaviorExtension : ValueSettingsBehaviour {
	fun createValueBox(bb: AABB, pos: BlockPos): ValueBox? = null
	
	fun addExtraTips(to: MutableList<MutableComponent>) {}
}
