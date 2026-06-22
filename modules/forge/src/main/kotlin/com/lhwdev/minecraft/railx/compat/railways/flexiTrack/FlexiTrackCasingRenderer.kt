package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.lhwdev.minecraft.railx.common.from
import com.lhwdev.minecraft.railx.compat.railways.RailwaysCasingExtension.isAlternate
import com.lhwdev.minecraft.railx.compat.railways.RailwaysCasingExtension.trackCasing
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils
import com.railwayteam.railways.registry.CRBlockPartials
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackShape
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.PI


@OnlyIn(Dist.CLIENT)
object FlexiTrackCasingRenderer {
	fun renderCasing(be: FlexiTrackBlockEntity, ms: PoseStack, buffer: MultiBufferSource, light: Int) {
		if(!FlexiTrackCasingUtils.isValidForCasing(be)) return
		val casing = be.trackCasing ?: return
		
		val vb = buffer.getBuffer(RenderType.cutoutMipped())
		ms.pushPose()
		
		if(be.isAlternate) {
			val texture = CasingRenderUtils.reTexture(CRBlockPartials.TRACK_CASING_FLAT_THICK, casing)
			for(pos in FlexiTrackCasingUtils.casingPositions(be)) {
				CachedBuffers.partial(texture, be.blockState)
					.translate(pos)
					.light<SuperByteBuffer>(light)
					.renderInto(ms, vb)
			}
		} else {
			val texture = CasingRenderUtils.reTexture(CRBlockPartials.TRACK_CASINGS[TrackShape.XO]!!.model, casing)
			for(axis in be.state.shapeCache) {
				CachedBuffers.partial(texture, be.blockState)
					.rotateCentered(axis.rotationValue)
					.scale(1.001f)
					.light<SuperByteBuffer>(light)
					.renderInto(ms, vb)
			}
		}
		
		ms.popPose()
	}
	
	fun renderTallBezierCasingsOld(
		ms: PoseStack,
		level: Level,
		state: BlockState,
		vb: VertexConsumer,
		bc: BezierConnection,
	) {
		val from = bc.from
		val segment = bc.bakedSegments
		val texture = CRBlockPartials.TRACK_CASINGS[TrackShape.XO]!!.getFor(bc.material.trackType).model
			.let { CasingRenderUtils.reTexture(it, bc.trackCasing ?: return) }
		
		for(index in 1..<segment.length) {
			// if(index % 2 == 0) continue
			val light = LevelRenderer.getLightColor(level, segment.lightPosition[index].offset(from))
			
			CachedBuffers.partial(texture, state)
				.mulPose(segment.tieTransform[index].pose())
				.rotateYCentered(PI.toFloat() / 2)
				.translate(0f, 0.001f * (index % 4) - 1 / 16f + 1 / 256f, 0f)
				.light<SuperByteBuffer>(light)
				.renderInto(ms, vb)
		}
	}
	
	fun renderTallBezierCasingsBlocky(
		ms: PoseStack,
		level: Level,
		state: BlockState,
		vb: VertexConsumer,
		bc: BezierConnection,
	) {
	
	}
}
