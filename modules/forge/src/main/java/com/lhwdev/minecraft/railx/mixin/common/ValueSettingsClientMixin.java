package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.ValueSettingsBehaviourExtra;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;


@Mixin(ValueSettingsClient.class)
public class ValueSettingsClientMixin {
	@WrapOperation(method = "tick", at = @At(value = "NEW", target = "(Lnet/minecraft/core/BlockPos;" +
		"Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueSettingsBoard;" +
		"Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueSettingsBehaviour$ValueSettings;" +
		"Ljava/util/function/Consumer;I)Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueSettingsScreen;",
		remap = false), remap = false)
	ValueSettingsScreen createSettingsScreen(
		BlockPos pos,
		ValueSettingsBoard board,
		ValueSettingsBehaviour.ValueSettings valueSettings,
		Consumer<ValueSettingsBehaviour.ValueSettings> onHover,
		int netId,
		Operation<ValueSettingsScreen> original,
		@Local(index = 5) ValueSettingsBehaviour behavior
	) {
		if(behavior instanceof ValueSettingsBehaviourExtra extra) {
			var screen = extra.createBoardScreen(pos, board, valueSettings, onHover, netId);
			if(screen != null) return screen;
		}
		
		return original.call(pos, board, valueSettings, onHover, netId);
	}
}
