package com.lhwdev.minecraft.railx.middleTrack.renderer

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.common.rendering.CommonTrackRenderer
import com.lhwdev.minecraft.railx.middleTrack.GlobalConnections
import com.lhwdev.minecraft.railx.utils.orFalse
import com.mojang.blaze3d.vertex.PoseStack
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVec3


@OnlyIn(Dist.CLIENT)
class MiddleTracksRenderer(level: Level) : CommonTrackRenderer(level) {
	companion object {
		fun renderAll(ms: PoseStack, buffer: MultiBufferSource, camera: Vec3) {
			if(!RailXConfig.Server.middleTrack.enabled.orFalse) return
			
			val minecraft = Minecraft.getInstance()
			val level = minecraft.level ?: return
			if(VisualizationManager.supportsVisualization(level)) return
			
			val renderer = MiddleTracksRenderer(level)
			
			renderer.renderTracks(camera = camera, ms = ms, bufferSource = buffer)
		}
	}
	
	
	fun renderTracks(camera: Vec3, ms: PoseStack, bufferSource: MultiBufferSource) {
		val vb = bufferSource.getBuffer(RenderType.cutoutMipped())
		
		for(connection in GlobalConnections[level]) {
			if(!connection.isActive) continue
			
			ms.pushPose()
			TransformStack.of(ms)
				.translate(connection.from.toVec3() - camera)
			
			renderTrack(level, bc = connection.curve, ms, vb)
			ms.popPose()
		}
	}
}
