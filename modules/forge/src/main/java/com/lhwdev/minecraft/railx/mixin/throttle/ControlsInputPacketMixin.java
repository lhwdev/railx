package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.throttle.ThrottlesServer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.UUID;


@Mixin(ControlsInputPacket.class)
public class ControlsInputPacketMixin {
	@WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions" +
		"/actors/trainControls/ControlsServerHandler;receivePressed(Lnet/minecraft/world/level/LevelAccessor;" +
		"Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;Lnet/minecraft/core/BlockPos;" +
		"Ljava/util/UUID;Ljava/util/Collection;Z)V"))
	void receivePressed(
		LevelAccessor world,
		AbstractContraptionEntity ace,
		BlockPos controlsPos,
		UUID uniqueId,
		Collection<Integer> activatedButtons,
		boolean press,
		Operation<Void> original
	) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isFalse()) {
			original.call(world, ace, controlsPos, uniqueId, activatedButtons, press);
			return;
		}
		ThrottlesServer.INSTANCE.receiveControlsInput(world, ace, controlsPos, uniqueId, activatedButtons, press);
	}
}
