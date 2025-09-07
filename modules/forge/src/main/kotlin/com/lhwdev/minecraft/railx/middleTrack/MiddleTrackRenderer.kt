package com.lhwdev.minecraft.railx.middleTrack

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.TrackRenderer
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider


class MiddleTrackRenderer(context: BlockEntityRendererProvider.Context) :
	SafeBlockEntityRenderer<MiddleTrackBlockEntity>() {
	override fun renderSafe(
		be: MiddleTrackBlockEntity,
		partialTicks: Float,
		ms: PoseStack,
		buffer: MultiBufferSource,
		light: Int,
		overlay: Int,
	) {
		val level = be.level!!
		val vb = buffer.getBuffer(RenderType.cutoutMipped())
		for(bc in be.connections) {
			TrackRenderer.renderBezierTurn(level, bc, ms, vb)
		}
	}
}