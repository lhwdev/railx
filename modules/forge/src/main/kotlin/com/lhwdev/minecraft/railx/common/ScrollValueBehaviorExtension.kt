package com.lhwdev.minecraft.railx.common

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import net.minecraft.network.chat.MutableComponent


interface ScrollValueBehaviorExtension : ValueSettingsBehaviour {
	fun addExtraTips(to: MutableList<MutableComponent>) {}
}
