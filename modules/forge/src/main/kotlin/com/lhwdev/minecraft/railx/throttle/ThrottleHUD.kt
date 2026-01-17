package com.lhwdev.minecraft.railx.throttle

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.network.chat.Component
import net.minecraft.world.level.GameType
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.abs


@OnlyIn(Dist.CLIENT)
object ThrottleHUD : LayeredDraw.Layer {
	override fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
		val mc = Minecraft.getInstance()
		if(mc.options.hideGui || mc.gameMode?.playerMode == GameType.SPECTATOR) return
		
		val entity = ControlsHandler.getContraption()
		if(entity !is CarriageContraptionEntity) return
		if(RailXConfig.Server.throttle.enabled.isFalse) return
		
		if(entity.carriage == null) return
		if(mc.cameraEntity == null) return
		if(ControlsHandler.getControlsPos() == null) return
		
		val throttle = ThrottlesClient.throttle ?: return
		val text = Component.empty()
		when(throttle.reverser) {
			Throttles.Reverser.Forward -> text.append("Forward")
			Throttles.Reverser.Neutral -> text.append("Neutral")
			Throttles.Reverser.Backward -> text.append("Backward")
		}
		text.append(" ")
		text.append(
			when {
				throttle.gear > 0 -> "F"
				throttle.gear == 0 -> "N"
				else -> "B"
			}
		)
		text.append("${abs(throttle.gear)}")
		
		graphics.drawString(mc.font, text, 16, graphics.guiHeight() - 29, 0xffffff)
	}
}
