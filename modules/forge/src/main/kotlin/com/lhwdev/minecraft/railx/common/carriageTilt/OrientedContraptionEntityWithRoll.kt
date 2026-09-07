@file:JvmName("OrientedContraptionEntityRollUtils")

package com.lhwdev.minecraft.railx.common.carriageTilt

import com.simibubi.create.content.contraptions.OrientedContraptionEntity

@Suppress("PropertyName")
interface OrientedContraptionEntityWithRoll {
	var `railx$prevRoll`: Float
	var `railx$roll`: Float
	
	fun `getRailx$viewZRot`(partialTicks: Float): Float
}


var OrientedContraptionEntity.prevRoll: Float
	get() = (this as OrientedContraptionEntityWithRoll).`railx$prevRoll`
	set(value) {
		(this as OrientedContraptionEntityWithRoll).`railx$prevRoll` = value
	}

var OrientedContraptionEntity.roll: Float
	get() = (this as OrientedContraptionEntityWithRoll).`railx$roll`
	set(value) {
		(this as OrientedContraptionEntityWithRoll).`railx$roll` = value
	}
