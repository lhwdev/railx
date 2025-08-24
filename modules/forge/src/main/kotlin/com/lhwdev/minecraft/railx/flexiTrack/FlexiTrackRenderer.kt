package com.lhwdev.minecraft.railx.flexiTrack

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.content.trains.track.TrackRenderer
import com.simibubi.create.content.trains.track.TrackShape
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.simibubi.create.AllBlocks as CreateBlocks


class FlexiTrackRenderer(context: BlockEntityRendererProvider.Context) : TrackRenderer(context) {
	override fun renderSafe(
		be: TrackBlockEntity,
		partialTicks: Float,
		ms: PoseStack,
		buffer: MultiBufferSource,
		light: Int,
		overlay: Int,
	) {
		be as FlexiTrackBlockEntity
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
		
		val level = be.level!!
		if(VisualizationManager.supportsVisualization(level)) return
		
		val vb = buffer.getBuffer(RenderType.CUTOUT_MIPPED)
		renderFlexiBlock(be, ms, vb, light)
	}
	
	fun renderFlexiBlock(be: FlexiTrackBlockEntity, ms: PoseStack, vb: VertexConsumer, light: Int) {
		val state = be.block.material.block.defaultBlockState().setValue(TrackBlock.SHAPE, TrackShape.XO)
		
		for(axis in be.shape.axes) {
			CachedBuffers.block(state)
				.light<SuperByteBuffer>(light)
				.rotateYCentered(axis.tangentAngle.toFloat())
				.renderInto(ms, vb)
		}
	}
}
