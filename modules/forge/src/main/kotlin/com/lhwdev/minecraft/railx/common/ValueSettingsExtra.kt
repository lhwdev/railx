package com.lhwdev.minecraft.railx.common

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen
import net.minecraft.core.BlockPos
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.util.function.Consumer


interface ValueSettingsBehaviourExtra : ValueSettingsBehaviour {
	/**
	 * @return `null` is for fallback.
	 */
	@OnlyIn(Dist.CLIENT)
	fun createBoardScreen(
		pos: BlockPos,
		board: ValueSettingsBoard,
		valueSettings: ValueSettingsBehaviour.ValueSettings,
		onHover: Consumer<ValueSettingsBehaviour.ValueSettings>,
		netId: Int,
	): ValueSettingsScreen? = null
}
