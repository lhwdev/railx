package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult


abstract class FlexiTrackRotateScrollBehavior(label: Component, be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	ScrollValueBehaviour(label, be, slot) {
	
	protected val be: FlexiTrackBlockEntity
		get() = blockEntity as FlexiTrackBlockEntity
}
