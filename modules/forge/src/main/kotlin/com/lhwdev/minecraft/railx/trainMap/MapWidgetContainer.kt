package com.lhwdev.minecraft.railx.trainMap

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import xaero.map.gui.GuiMap
import java.util.*


private typealias Widget = AbstractWidget


open class MapWidgetContainer<E : MapWidgetContainer.Entry<*>>(val map: GuiMap) :
	AbstractContainerWidget(0, 0, map.width, map.height, Component.empty()) {
	
	val children: MutableList<E> = mutableListOf()
	
	var hovered: E? = null
		private set
	
	var offsetX = 0
		private set
	
	var offsetY = 0
		private set
	
	val bounds: Rect2i
		get() = Rect2i(offsetX, offsetY, width, height)
	
	
	override fun getFocused(): E? =
		@Suppress("UNCHECKED_CAST") (super.focused as E?)
	
	override fun children(): List<E> =
		children
	
	fun addChild(child: E) {
		children += child
	}
	
	
	override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		hovered = childAt(mouseX.toDouble(), mouseY.toDouble())
		
		val offsetX = offsetX
		val offsetY = offsetY
		val width = width
		val height = height
		
		val pose = guiGraphics.pose()
		pose.pushPose()
		pose.translate(-offsetX.toFloat(), -offsetY.toFloat(), 0f)
		
		for(child in children) {
			child.hovered = hovered === child
			if(!child.isInScreen(offsetX, offsetY, width, height)) continue
			child.render(guiGraphics, mouseX + offsetX, mouseY + offsetY, partialTick)
		}
		
		pose.popPose()
	}
	
	
	override fun getChildAt(mouseX: Double, mouseY: Double): Optional<GuiEventListener> =
		super.getChildAt(mouseX + offsetX, mouseY + offsetY)
	
	fun childAt(mouseX: Double, mouseY: Double): E? {
		for(child in this.children()) {
			if(child.isMouseOver(mouseX, mouseY)) return child
		}
		return null
	}
	
	override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
		super.mouseClicked(mouseX + offsetX, mouseY + offsetY, button)
	
	override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean =
		super.mouseReleased(mouseX + offsetX, mouseY + offsetY, button)
	
	override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean =
		super.mouseDragged(mouseX + offsetX, mouseY + offsetY, button, dragX, dragY)
	
	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
		super.mouseScrolled(mouseX + offsetX, mouseY + offsetY, scrollX, scrollY)
	
	
	override fun updateWidgetNarration(output: NarrationElementOutput) {
		hovered?.let {
			it.updateNarration(output)
			narrateItemPosition(output, it)
			return
		}
		focused?.let {
			it.updateNarration(output)
			narrateItemPosition(output, it)
			return
		}
	}
	
	private fun narrateItemPosition(output: NarrationElementOutput, entry: E) {
		if(children.size <= 1) return
		output.add(NarratedElementType.POSITION, "Anchored at pos [${entry.x}, ${entry.y}]")
	}
	
	
	open class Entry<E : Widget>(val child: E, message: Component) :
		AbstractContainerWidget(child.x, child.y, child.width, child.height, message) {
		
		constructor(child: E) : this(child, child.message)
		
		var hovered: Boolean = false
			internal set
		
		private val children = listOf(child)
		
		override fun children(): List<GuiEventListener> =
			children
		
		override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
			x = child.x
			y = child.y
			width = child.width
			height = child.height
			
			child.render(guiGraphics, mouseX, mouseY, partialTick)
		}
		
		
		open fun isInScreen(offsetX: Int, offsetY: Int, screenWidth: Int, screenHeight: Int): Boolean =
			!(x + width < offsetX || x > offsetX + screenWidth ||
				y + height < offsetY || y > offsetY + screenHeight)
		
		open fun hitTest(x: Double, y: Double): Boolean =
			x >= 0 && x <= width && y >= 0 && y <= height
		
		
		override fun getChildAt(mouseX: Double, mouseY: Double): Optional<GuiEventListener> =
			if(hitTest(mouseX, mouseY)) Optional.of(child) else Optional.empty()
		
		override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
			if(hitTest(mouseX, mouseY)) super.mouseClicked(mouseX, mouseY, button) else false
		
		override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean =
			if(hitTest(mouseX, mouseY)) super.mouseReleased(mouseX, mouseY, button) else false
		
		override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean =
			if(hitTest(mouseX, mouseY)) super.mouseDragged(mouseX, mouseY, button, dragX, dragY) else false
		
		override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
			if(hitTest(mouseX, mouseY)) super.mouseScrolled(mouseX, mouseY, scrollX, scrollY) else false
		
		
		override fun updateWidgetNarration(output: NarrationElementOutput) {
			child.updateNarration(output)
		}
	}
	
	open class InfiniteEntry<E : Widget>(child: E, message: Component) : Entry<E>(child, message) {
		override fun isInScreen(offsetX: Int, offsetY: Int, screenWidth: Int, screenHeight: Int): Boolean = true
		
		override fun hitTest(x: Double, y: Double): Boolean = true
	}
}


fun <T : Widget> MapWidgetContainer<MapWidgetContainer.Entry<T>>.addChild(child: T) {
	addChild(MapWidgetContainer.Entry(child, child.message))
}

fun <T : Widget> MapWidgetContainer<MapWidgetContainer.Entry<T>>.addChildren(vararg children: T) {
	this.children += children.map { MapWidgetContainer.Entry(it, it.message) }
}
