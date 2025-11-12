package com.lhwdev.minecraft.railx.middleTrack.renderer

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.common.from
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.lhwdev.minecraft.railx.middleTrack.GlobalConnections
import com.lhwdev.minecraft.railx.utils.getOrNull
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.trains.track.BezierConnection
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.createmod.catnip.theme.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3
import kotlin.math.min
import kotlin.random.Random

@OnlyIn(Dist.CLIENT)
class MiddleTracksRenderer(val level: Level) {
	companion object {
		fun renderAll(ms: PoseStack, buffer: MultiBufferSource, camera: Vec3) {
			if(RailXConfig.Server.middleTrack.enabled.getOrNull() != true) return
			
			val minecraft = Minecraft.getInstance()
			val level = minecraft.level ?: return
			if(VisualizationManager.supportsVisualization(level)) return
			
			val renderer = MiddleTracksRenderer(level)
			
			renderer.renderTracks(
				camera = camera,
				poseStack = ms,
				bufferSource = buffer,
			)
		}
	}
	
	
	fun renderTracks(camera: Vec3, poseStack: PoseStack, bufferSource: MultiBufferSource) {
		val vb = bufferSource.getBuffer(RenderType.cutoutMipped())
		
		for(connection in GlobalConnections[level]) {
			if(!connection.isActive) continue
			
			poseStack.pushPose()
			TransformStack.of(poseStack)
				.translate(connection.from.toVec3() - camera)
			
			renderTrack(level, connection, poseStack, vb)
			poseStack.popPose()
		}
	}
	
	fun renderTrack(level: Level, connection: ConnectionMiddleState, ms: PoseStack, vb: VertexConsumer) {
		val bc = connection.curve
		
		ms.pushPose()
		val from = bc.from
		val air = Blocks.AIR.defaultBlockState()
		val segment = bc.bakedSegments
		
		renderGirder(level, bc, ms, vb, from)
		
		val random = Random(bc.bePositions.first.asLong())
		
		for(i in 1..<segment.length) {
			val light = LevelRenderer.getLightColor(level, segment.lightPosition[i].offset(from))
			
			val modelHolder = bc.material.modelHolder
			
			CachedBuffers.partial(modelHolder.tie, air).apply {
				mulPose(segment.tieTransform[i].pose())
				mulNormal(segment.tieTransform[i].normal())
				rotateYCentered(random.nextFloat() * 0.15f)
				var color = Color.mixColors(0x6666ff, 0xff6666, min(1f, connection.allMiddles.size * 0.1f))
				val half = segment.length / 2
				if(i < half)
					color = Color.mixColors(0xffffff, color, (-0.2f + 0.9f * i / half).coerceAtMost(1f))
				else if(i > half)
					color = Color.mixColors(color, 0x000000, (-0.3f + 1.0f * (i - half) / half).coerceAtLeast(0f))
				color<SuperByteBuffer>(color)
				light<SuperByteBuffer>(light)
				renderInto(ms, vb)
			}
			
			for(first in Iterate.trueAndFalse) {
				val transform = segment.railTransforms[i][first]
				CachedBuffers.partial(if(first) modelHolder.leftSegment else modelHolder.rightSegment, air).apply {
					mulPose(transform.pose())
					mulNormal(transform.normal())
					if(bc.material is FlexiTrackMaterial) color<SuperByteBuffer>(0xaaffaa)
					light<SuperByteBuffer>(light)
					renderInto(ms, vb)
				}
			}
		}
		
		ms.popPose()
	}
	
	private fun renderGirder(
		level: Level, bc: BezierConnection, ms: PoseStack, vb: VertexConsumer,
		tePosition: BlockPos,
	) {
		if(!bc.hasGirder) return
		
		val air = Blocks.AIR.defaultBlockState()
		val segment = bc.getBakedGirders()
		
		for(i in 1..<segment.length) {
			val light = LevelRenderer.getLightColor(level, segment.lightPosition[i].offset(tePosition))
			
			for(first in Iterate.trueAndFalse) {
				val beamTransform = segment.beams[i][first]
				CachedBuffers.partial(AllPartialModels.GIRDER_SEGMENT_MIDDLE, air)
					.mulPose(beamTransform.pose())
					.mulNormal(beamTransform.normal())
					.light<SuperByteBuffer>(light)
					.renderInto(ms, vb)
				
				for(top in Iterate.trueAndFalse) {
					val beamCapTransform = segment.beamCaps[i][top][first]
					CachedBuffers.partial(
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
}
