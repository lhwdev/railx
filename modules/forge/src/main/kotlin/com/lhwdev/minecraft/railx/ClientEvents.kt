package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.common.PreciseTrackPlacementOverlay
import com.lhwdev.minecraft.railx.middleTrack.CurvedMiddleTrackInteraction
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackOutline
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackTargetingClient
import com.lhwdev.minecraft.railx.middleTrack.renderer.MiddleTracksRenderer
import com.lhwdev.minecraft.railx.throttle.ThrottleHUD
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.gui.VanillaGuiLayers


@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object ClientEvents {
	@SubscribeEvent
	fun onTickPost(event: ClientTickEvent.Post) {
		MiddleTrackTargetingClient.clientTick()
	}
	
	
	@SubscribeEvent
	fun onClickInput(event: InputEvent.InteractionKeyMappingTriggered) {
		var result = event.isCanceled
		
		if(!result) result = CurvedMiddleTrackInteraction.onClickInput(event)
		
		if(result) event.isCanceled = true
	}
	
	
	@SubscribeEvent
	fun registerGuiOverlays(event: RegisterGuiLayersEvent) {
		event.registerAbove(
			VanillaGuiLayers.EXPERIENCE_BAR,
			RailX.asResource("throttle_hud"),
			ThrottleHUD
		)
		event.registerAbove(
			VanillaGuiLayers.HOTBAR,
			RailX.asResource("precise_track_placement"),
			PreciseTrackPlacementOverlay
		)
	}
	
	@SubscribeEvent
	fun onPreRenderGui(event: RenderGuiEvent.Pre) {
		PreciseTrackPlacementOverlay.onPreRender(event)
	}
	
	@SubscribeEvent
	fun renderWorld(event: RenderLevelStageEvent) {
		val renderer = renderStages[event.stage] ?: return
		
		val ms = event.poseStack
		val buffer = Minecraft.getInstance().renderBuffers().bufferSource()
		val camera = event.camera.position
		renderer(ms, buffer, camera)
	}
	
	private val renderStages = mapOf<RenderLevelStageEvent.Stage, Renderer>(
		RenderLevelStageEvent.Stage.AFTER_PARTICLES to { ms, buffer, camera ->
			MiddleTrackOutline.drawCurveSelection(ms, buffer, camera)
			MiddleTrackTargetingClient.render(ms, buffer, camera)
		},
		RenderLevelStageEvent.Stage.AFTER_ENTITIES to { ms, buffer, camera ->
			MiddleTracksRenderer.renderAll(ms, buffer, camera)
		},
	)
}

typealias Renderer = (ms: PoseStack, buffer: MultiBufferSource, camera: Vec3) -> Unit
