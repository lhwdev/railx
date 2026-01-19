package com.lhwdev.minecraft.railx.realisticSpeed.control

import com.lhwdev.minecraft.railx.RailXConfig
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.level.GameType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay


@OnlyIn(Dist.CLIENT)
object RealisticSpeedHUD : IGuiOverlay {
	override fun render(gui: ForgeGui, graphics: GuiGraphics, partialTicks: Float, width: Int, height: Int) {
		val mc = Minecraft.getInstance()
		if(mc.options.hideGui || mc.gameMode?.playerMode == GameType.SPECTATOR) return
		if(!RailXConfig.Server.flexiTrak.enabled.get()) return
		
		val entity = ControlsHandler.getContraption()
		if(entity !is CarriageContraptionEntity) return
		
		if(entity.carriage == null) return
		if(mc.cameraEntity == null) return
		val parameters = RealisticSpeedClient.parameters ?: return
		
		val text = Component.empty()
		if(parameters.slip) text.append("Slip")
		
		graphics.drawString(
			mc.font,
			text,
			graphics.guiWidth() - 16 - mc.font.width(text),
			graphics.guiHeight() - 29,
			0xffffff
		)
	}
}
