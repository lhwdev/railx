package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.common.PreciseTrackPlacementOverlay
import com.lhwdev.minecraft.railx.flexiTrack.CurvedFlexiTrackInteraction
import com.lhwdev.minecraft.railx.middleTrack.CurvedMiddleTrackInteraction
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackOutline
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackTargetingClient
import com.lhwdev.minecraft.railx.middleTrack.renderer.MiddleTracksRenderer
import com.lhwdev.minecraft.railx.throttle.ThrottleHUD
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber


@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object ClientEvents {
	@SubscribeEvent
	fun onTickPost(event: TickEvent.ClientTickEvent) {
		if(event.phase != TickEvent.Phase.END) return
		MiddleTrackTargetingClient.clientTick()
	}
	
	
	@SubscribeEvent
	fun onClickInput(event: InputEvent.InteractionKeyMappingTriggered) {
		var result = event.isCanceled
		
		if(!result) result = CurvedMiddleTrackInteraction.onClickInput(event)
		
		if(!result) result = CurvedFlexiTrackInteraction.onClickInput(event)
		
		if(result) event.isCanceled = true
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


@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
object ClientModBusEvents {
	@SubscribeEvent
	fun registerGuiOverlays(event: RegisterGuiOverlaysEvent) {
		event.registerAbove(
			VanillaGuiOverlay.EXPERIENCE_BAR.id(),
			"throttle_hud",
			ThrottleHUD
		)
		event.registerAbove(
			VanillaGuiOverlay.HOTBAR.id(),
			"precise_track_placement",
			PreciseTrackPlacementOverlay
		)
	}
}

typealias Renderer = (ms: PoseStack, buffer: MultiBufferSource, camera: Vec3) -> Unit
