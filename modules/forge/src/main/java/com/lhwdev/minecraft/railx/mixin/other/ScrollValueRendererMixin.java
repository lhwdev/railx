package com.lhwdev.minecraft.railx.mixin.other;

import com.lhwdev.minecraft.railx.common.ScrollValueBehaviorExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;


@Mixin(ScrollValueRenderer.class)
public class ScrollValueRendererMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity" +
		"/behaviour/ValueSettingsClient;showHoverTip(Ljava/util/List;)V"))
	private static void showHoverTip(
		ValueSettingsClient instance,
		List<MutableComponent> tip,
		Operation<Void> original,
		@Local(index = 10) ScrollValueBehaviour behavior
	) {
		if(behavior instanceof ScrollValueBehaviorExtension extension)
			extension.addExtraTips(tip);
		original.call(instance, tip);
	}
}
