package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.common.ScrollValueBehaviorExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(value = ScrollValueRenderer.class, priority = 5000)
public class ScrollValueRendererMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity" +
		"/behaviour/ValueSettingsClient;showHoverTip(Ljava/util/List;)V"), require = 0)
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
	
	@Inject(method = "addBox", at = @At("HEAD"), cancellable = true)
	private static void addBox(
		ClientLevel world,
		BlockPos pos,
		Direction face,
		ScrollValueBehaviour behaviour,
		boolean highlight,
		CallbackInfo ci
	) {
		if(behaviour instanceof ScrollValueBehaviorExtension extension) {
			AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.5f)
				.contract(0, 0, -.5f)
				.move(0, 0, -.125f);
			
			ValueBox box = extension.createValueBox(bb, pos);
			if(box != null) {
				ci.cancel();
				
				box.passive(!highlight)
					.wideOutline();
				
				Outliner.getInstance().showOutline(behaviour, box.transform(behaviour.getSlotPositioning()))
					.highlightFace(face);
			}
		}
	}
}
