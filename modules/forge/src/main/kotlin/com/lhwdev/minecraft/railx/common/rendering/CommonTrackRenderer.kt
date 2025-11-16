package com.lhwdev.minecraft.railx.common.rendering

import com.lhwdev.minecraft.railx.common.from
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn


@OnlyIn(Dist.CLIENT)
open class CommonTrackRenderer(protected val level: Level) {
	open fun renderTrack(level: Level, bc: BezierConnection, ms: PoseStack, vb: VertexConsumer) {
		ms.pushPose()
		
		renderGirder(level, bc, ms, vb)
		
		val segments = bc.bakedSegments
		for(index in 1..<segments.length) {
			renderTrackSegment(level, bc, ms, vb, segments, index)
		}
		
		ms.popPose()
	}
	
	open fun renderTrackSegment(
		level: Level,
		bc: BezierConnection,
		ms: PoseStack,
		vb: VertexConsumer,
		segments: BezierConnection.SegmentAngles,
		index: Int,
	) {
		val air = Blocks.AIR.defaultBlockState()
		val light = LevelRenderer.getLightColor(level, segments.lightPosition[index].offset(bc.from))
		val modelHolder = bc.material.modelHolder
		
		CachedBuffers.partial(modelHolder.tie, air).apply {
			mulPose(segments.tieTransform[index].pose())
			mulNormal(segments.tieTransform[index].normal())
			light<SuperByteBuffer>(light)
			renderInto(ms, vb)
		}
		
		for(first in Iterate.trueAndFalse) {
			val transform = segments.railTransforms[index][first]
			CachedBuffers.partial(if(first) modelHolder.leftSegment else modelHolder.rightSegment, air).apply {
				mulPose(transform.pose())
				mulNormal(transform.normal())
				light<SuperByteBuffer>(light)
				renderInto(ms, vb)
			}
		}
	}
	
	open fun renderGirder(level: Level, bc: BezierConnection, ms: PoseStack, vb: VertexConsumer) {
		if(!bc.hasGirder) return
		
		val segments = bc.bakedGirders
		for(index in 1..<segments.length) {
			renderGirderSegment(bc, ms, vb, segments, index)
		}
	}
	
	open fun renderGirderSegment(
		bc: BezierConnection,
		ms: PoseStack,
		vb: VertexConsumer,
		segments: BezierConnection.GirderAngles,
		index: Int,
	) {
		val air = Blocks.AIR.defaultBlockState()
		val light = LevelRenderer.getLightColor(level, segments.lightPosition[index].offset(bc.from))
		
		for(first in Iterate.trueAndFalse) {
			val beamTransform = segments.beams[index][first]
			CachedBuffers.partial(AllPartialModels.GIRDER_SEGMENT_MIDDLE, air)
				.mulPose(beamTransform.pose())
				.mulNormal(beamTransform.normal())
				.light<SuperByteBuffer>(light)
				.renderInto(ms, vb)
			
			val beamCaps = segments.beamCaps[index]
			for(top in Iterate.trueAndFalse) {
				val beamCapTransform = beamCaps[top][first]
				CachedBuffers
					.partial(
						if(top) AllPartialModels.GIRDER_SEGMENT_TOP else AllPartialModels.GIRDER_SEGMENT_BOTTOM,
						air
					)
					.mulPose(beamCapTransform.pose())
					.mulNormal(beamCapTransform.normal())
					.light<SuperByteBuffer>(light)
					.renderInto(ms, vb)
			}
		}
	}
}
