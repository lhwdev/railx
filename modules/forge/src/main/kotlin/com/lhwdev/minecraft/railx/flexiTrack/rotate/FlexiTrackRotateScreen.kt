package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.BlockHitResult
import java.lang.invoke.MethodHandles
import java.util.function.Consumer


class FlexiTrackRotateScreen(
	val behavior: FlexiTrackRotateScrollBehaviors,
	private val hitResult: BlockHitResult,
	pos: BlockPos,
	board: ValueSettingsBoard,
	valueSettings: ValueSettingsBehaviour.ValueSettings,
	onHover: Consumer<ValueSettingsBehaviour.ValueSettings>,
	netId: Int,
) : ValueSettingsScreen(pos, board, valueSettings, onHover, netId) {
	
	private object Reflection {
		private val lookup = MethodHandles.lookup()
		
		val setBoard = ValueSettingsScreen::class.java.getDeclaredField("board")
			.also { it.isAccessible = true }
			.let { lookup.unreflectSetter(it) }
		
		val setInitialSettings = ValueSettingsScreen::class.java.getDeclaredField("initialSettings")
			.also { it.isAccessible = true }
			.let { lookup.unreflectSetter(it) }
		
		val getIconMode = ValueSettingsScreen::class.java.getDeclaredField("iconMode")
			.also { it.isAccessible = true }
			.let { lookup.unreflectGetter(it) }
	}
	
	
	private var ticksOpen = 0
	
	private var precise: Boolean = hasControlDown()
	
	private var initialSettings: ValueSettingsBehaviour.ValueSettings
		get() = error("stub")
		set(value) {
			Reflection.setInitialSettings.invokeExact(this as ValueSettingsScreen, value)
		}
	
	private var board: ValueSettingsBoard
		get() = error("stub")
		set(value) {
			Reflection.setBoard.invokeExact(this as ValueSettingsScreen, value)
		}
	
	private val iconMode
		get() = Reflection.getIconMode.invokeExact(this as ValueSettingsScreen) as Boolean
	
	override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
		val mc = minecraft!!
		val newPrecise = hasControlDown()
		if(newPrecise != precise) {
			precise = newPrecise
			board = behavior.createBoard(mc.player!!, hitResult)
			initialSettings = behavior.valueSettings
			init()
		}
		
		super.renderWindow(graphics, mouseX, mouseY, partialTicks)
		
		if(ticksOpen < 2) return
		val precise = hasControlDown()
		val text = Component.literal("Hold ")
			.append(
				Component.keybind("key.sprint")
					.withStyle(if(precise) ChatFormatting.GREEN else ChatFormatting.WHITE)
			)
			.append(" to Precisely Rotate")
		
		
		val textX = guiLeft + windowWidth / 2 - font.width(text) / 2
		val additionalHeight = if(iconMode) 46 else 33
		graphics.drawString(font, text, textX, guiTop + windowHeight + additionalHeight - 15, 0xdddddd, false)
	}
	
	override fun tick() {
		super.tick()
		ticksOpen++
	}
}
