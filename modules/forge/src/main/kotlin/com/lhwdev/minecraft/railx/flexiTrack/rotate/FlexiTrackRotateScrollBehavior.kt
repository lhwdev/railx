package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.minecraft.network.chat.Component


abstract class FlexiTrackRotateScrollBehavior(label: Component, be: FlexiTrackBlockEntity, slot: ValueBoxTransform) :
	ScrollValueBehaviour(label, be, slot) {
	
	abstract val kind: FlexiTrackRotateScrollBehaviors.Kind
	
	protected val be: FlexiTrackBlockEntity
		get() = blockEntity as FlexiTrackBlockEntity
}
