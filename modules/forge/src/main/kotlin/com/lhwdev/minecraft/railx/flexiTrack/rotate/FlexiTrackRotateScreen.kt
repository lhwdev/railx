package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.rotate.FlexiTrackRotateScrollBehavior.RotateStepMode
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler
import net.createmod.catnip.animation.Force
import net.createmod.catnip.animation.PhysicalFloat
import net.createmod.catnip.platform.CatnipServices
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.BlockHitResult
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.function.Consumer


class FlexiTrackRotateScreen(
	val behavior: FlexiTrackRotateScrollBehaviors,
	private val hitResult: BlockHitResult,
	val pos: BlockPos,
	board: ValueSettingsBoard,
	valueSettings: ValueSettingsBehaviour.ValueSettings,
	onHover: Consumer<ValueSettingsBehaviour.ValueSettings>,
	netId: Int,
) : ValueSettingsScreen(pos, board, valueSettings, onHover, netId) {
	
	private object Reflection {
		private val lookup = MethodHandles.lookup()
		
		val getLastHovered: MethodHandle = ValueSettingsScreen::class.java.getDeclaredField("lastHovered")
			.also { it.isAccessible = true }
			.let { lookup.unreflectGetter(it) }
		
		val setBoard: MethodHandle = ValueSettingsScreen::class.java.getDeclaredField("board")
			.also { it.isAccessible = true }
			.let { lookup.unreflectSetter(it) }
		
		val setInitialSettings: MethodHandle = ValueSettingsScreen::class.java.getDeclaredField("initialSettings")
			.also { it.isAccessible = true }
			.let { lookup.unreflectSetter(it) }
		
		val getIconMode: MethodHandle = ValueSettingsScreen::class.java.getDeclaredField("iconMode")
			.also { it.isAccessible = true }
			.let { lookup.unreflectGetter(it) }
		
		val PhysicalFloat_getForces: MethodHandle = PhysicalFloat::class.java.getDeclaredField("forces")
			.also { it.isAccessible = true }
			.let { lookup.unreflectGetter(it) }
	}
	
	
	private var ticksOpen = 0
	private var soundCooldown = 0
	private var lastHovered = ValueSettingsBehaviour.ValueSettings(-1, -1)
	
	private var currentMode: RotateStepMode = behavior.modeClient
	
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
	
	override fun saveAndClose(x: Double, y: Double) {
		val settings = getClosestCoordinate(x.toInt(), y.toInt())
		val configurePacket = ConfigureFlexiTrackRotatePacket.save(pos, settings.value(), currentMode)
		CatnipServices.NETWORK.sendToServer(configurePacket)
		onClose()
	}
	
	override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
		val mc = minecraft!!
		val newMode = behavior.modeClient
		if(newMode != currentMode) {
			currentMode = newMode
			board = behavior.createBoard(mc.player!!, hitResult, mode = newMode)
			initialSettings = behavior.valueSettings
			init()
		}
		
		super.renderWindow(graphics, mouseX, mouseY, partialTicks)
		
		// allows reversing wrench rotation
		val closest = Reflection.getLastHovered.invokeExact(this as ValueSettingsScreen)
			as ValueSettingsBehaviour.ValueSettings
		if(closest != lastHovered && soundCooldown == 0) {
			@Suppress("UNCHECKED_CAST")
			val forces = Reflection.PhysicalFloat_getForces
				.invokeExact(ScrollValueHandler.wrenchCog as PhysicalFloat)
				as ArrayList<Force>
			
			forces.removeLastOrNull()
			
			val forceSign = if(behavior.scrollDirection == FlexiTrackRotateScrollBehavior.ScrollDirection.Descending) {
				1
			} else {
				-1
			}
			val forceMultiplier = when(newMode) {
				RotateStepMode.Normal -> 10.0
				RotateStepMode.Precise -> if(forces.size >= 2) 1.0 else 8.0
				RotateStepMode.Coarse -> 15.0
			}
			ScrollValueHandler.wrenchCog.bump(3, forceSign * (closest.value - lastHovered.value) * forceMultiplier)
			soundCooldown = 1
		}
		lastHovered = closest
		
		if(ticksOpen < 2) return
		
		fun formatMode(mode: RotateStepMode, name: String, keybind: String): MutableComponent {
			val selectionStyle = if(mode == newMode) ChatFormatting.GREEN else ChatFormatting.WHITE
			return Component.empty()
				.append(
					Component.literal(name).withStyle(selectionStyle)
				)
				.append(" (")
				.append(Component.keybind(keybind).withStyle(selectionStyle))
				.append(")")
		}
		
		val text = Component.empty().apply {
			append(formatMode(mode = RotateStepMode.Precise, name = "Precise", keybind = "key.keyboard.left.control"))
			if(behavior.hasCoarseMode) {
				append(" | ")
				append(formatMode(mode = RotateStepMode.Coarse, name = "Coarse", keybind = "key.keyboard.left.alt"))
			}
			append(" Mode")
		}
		
		val textX = guiLeft + windowWidth / 2 - font.width(text) / 2
		val additionalHeight = if(iconMode) 46 else 33
		graphics.drawString(font, text, textX, guiTop + windowHeight + additionalHeight - 15, 0xdddddd, false)
	}
	
	override fun tick() {
		super.tick()
		ticksOpen++
		if(soundCooldown > 0) soundCooldown--
	}
}
