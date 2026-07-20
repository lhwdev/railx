package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockOutline;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;


@Mixin(value = TrackBlockOutline.class, remap = false)
public class TrackBlockOutlineMixin {
	@Inject(method = "drawCustomBlockSelection", at = @At("HEAD"), cancellable = true)
	private static void onDrawCustomBlockSelection(RenderHighlightEvent.Block event, CallbackInfo ci) {
		if(FlexiTrackBlockOutline.INSTANCE.drawCustomBlockSelection(event)) {
			ci.cancel();
		}
	}
	
	@WrapOperation(method = "drawCurveSelection", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content" +
		"/trains/track/TrackBlockOutline;renderShape(Lnet/minecraft/world/phys/shapes/VoxelShape;" +
		"Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/lang/Boolean;)V"))
	private static void renderShape(
		VoxelShape s,
		PoseStack ms,
		VertexConsumer vb,
		Boolean valid,
		Operation<Void> original
	) {
		if(RailXConfig.Client.Value.getFlexiTrak().getRightClickCurveToCopy().isFalse()) {
			original.call(s, ms, vb, valid);
			return;
		}
		
		ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
		if(stack.is(com.simibubi.create.AllBlocks.CLIPBOARD.asItem())) {
			List<MutableComponent> tip = new ArrayList<>();
			tip.add(CreateLang.translateDirect("clipboard.actions"));
			tip.add(CreateLang.translateDirect("clipboard.copy_other_clipboard", Component.keybind("key.use")));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
			valid = true;
		}
		
		original.call(s, ms, vb, valid);
	}
}
