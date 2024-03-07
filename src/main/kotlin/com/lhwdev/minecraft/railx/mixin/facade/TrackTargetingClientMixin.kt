@file:JvmName("TrackTargetingClientMixin")
@file:Mixin(TrackTargetingClient::class)

package com.lhwdev.minecraft.railx.mixin.facade

import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.TrainNetworkObserverBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.track.TrackTargetingClient
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


@Shadow(remap = false)
private var lastType: EdgePointType<*>? = null

@Inject(method = ["render"], at = [At("HEAD")], remap = false)
private fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, ci: CallbackInfo) {
	if(lastType === TrainNetworkObserverBlockEntity.NETWORK_OBSERVER) lastType = EdgePointType.OBSERVER
}
