package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.common.PreciseTrackPlacementOverlay
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackOutline
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers


@EventBusSubscriber(Dist.CLIENT)
object ClientEvents {
	@SubscribeEvent
	fun registerGuiOverlays(event: RegisterGuiLayersEvent) {
		event.registerAbove(
			VanillaGuiLayers.HOTBAR,
			RailX.asResource("precise_track_placement"),
			PreciseTrackPlacementOverlay
		)
	}
	
	@SubscribeEvent
	fun renderWorld(event: RenderLevelStageEvent) {
		if(event.stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return
		
		val ms = event.poseStack
		val buffer = Minecraft.getInstance().renderBuffers().bufferSource()
		val camera = event.camera.position
		MiddleTrackOutline.drawCurveSelection(ms, buffer, camera)
	}
}
