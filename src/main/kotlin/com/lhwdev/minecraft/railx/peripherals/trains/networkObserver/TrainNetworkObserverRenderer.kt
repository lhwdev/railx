package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class TrainNetworkObserverRenderer(context: BlockEntityRendererProvider.Context?) :
	SmartBlockEntityRenderer<TrainNetworkObserverBlockEntity>(context) {
	
	override fun renderSafe(
		te: TrainNetworkObserverBlockEntity,
		partialTicks: Float,
		ms: PoseStack,
		buffer: MultiBufferSource,
		light: Int,
		overlay: Int,
	) {
		super.renderSafe(te, partialTicks, ms, buffer, light, overlay)
		val pos = te.blockPos
		
		val target = te.edgePoint
		val targetPosition = target.globalPosition
		val level = te.level!!
		
		val trackState = level.getBlockState(targetPosition)
		val block = trackState.block
		if(block !is ITrackBlock) return
		
		ms.pushPose()
		ms.translate(-pos.x.toFloat(), -pos.y.toFloat(), -pos.z.toFloat())
		val type = RenderedTrackOverlayType.OBSERVER
		TrackTargetingBehaviour.render(
			level, targetPosition, target.targetDirection, target.targetBezier, ms,
			buffer, light, overlay, type, 1f
		)
		ms.popPose()
	}
}
