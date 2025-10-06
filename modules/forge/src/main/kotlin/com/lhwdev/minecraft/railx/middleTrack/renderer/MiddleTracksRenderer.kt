package com.lhwdev.minecraft.railx.middleTrack.renderer

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.middleTrack.GlobalConnections
import com.lhwdev.minecraft.railx.utils.getOrNull
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.TrackRenderer
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3

@OnlyIn(Dist.CLIENT)
class MiddleTracksRenderer(val level: Level) {
	@EventBusSubscriber(Dist.CLIENT, modid = RailX.Companion.Id)
	companion object {
		@SubscribeEvent
		private fun afterRenderEntities(event: RenderLevelStageEvent) {
			if(event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
			if(RailXConfig.Server.middleTrack.enabled.getOrNull() != true) return
			
			val minecraft = Minecraft.getInstance()
			val level = minecraft.level ?: return
			if(VisualizationManager.supportsVisualization(level)) return
			
			val renderer = MiddleTracksRenderer(level)
			
			renderer.renderTracks(
				camera = event.camera,
				poseStack = event.poseStack,
				bufferSource = minecraft.renderBuffers().bufferSource(),
			)
		}
	}
	
	
	fun renderTracks(camera: Camera, poseStack: PoseStack, bufferSource: MultiBufferSource) {
		val vb = bufferSource.getBuffer(RenderType.cutoutMipped())
		
		for(connection in GlobalConnections[level]) {
			if(!connection.isActive) continue
			
			poseStack.pushPose()
			TransformStack.of(poseStack)
				.translate(connection.from.toVec3() - camera.position)
			
			TrackRenderer.renderBezierTurn(level, connection.curve, poseStack, vb)
			poseStack.popPose()
		}
	}
}
