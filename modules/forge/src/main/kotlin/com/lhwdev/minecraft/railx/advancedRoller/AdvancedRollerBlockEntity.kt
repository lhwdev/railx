package com.lhwdev.minecraft.railx.advancedRoller

import com.copycatsplus.copycats.content.copycat.layer.CopycatLayerBlock
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock
import com.lhwdev.minecraft.railx.common.ScrollValueBehaviorExtension
import com.lhwdev.minecraft.railx.common.gravelLayer.GravelLayerBlock
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.railwayteam.railways.registry.CRIcons
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.*
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import java.lang.invoke.MethodHandles

class AdvancedRollerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	RollerBlockEntity(type, pos, state) {
	
	companion object {
		private val setMode = RollerBlockEntity::class.java.getDeclaredField("mode")
			.let { MethodHandles.lookup().unreflectSetter(it) }
	}
	
	lateinit var newMode: RollerScrollBehavior
	
	sealed class AdvancedRollingMode(
		@get:JvmName("getIconJvm") val icon: AllIcons,
		@get:JvmName("getTranslationKeyJvm") val translationKey: String,
	) : INamedIconOptions {
		companion object {
			val Options: List<AdvancedRollingMode> = buildList {
				add(TunnelPave)
				add(StraightFill)
				add(WideFill)
				
				if(CompatMods.railways) {
					add(RailwayTrackReplace)
				}
				
				add(SmoothWideFill)
			}
		}
		
		object TunnelPave : AdvancedRollingMode(
			icon = AllIcons.I_ROLLER_PAVE,
			translationKey = "create.contraptions.roller_mode.tunnel_pave"
		)
		
		object StraightFill : AdvancedRollingMode(
			icon = AllIcons.I_ROLLER_FILL,
			translationKey = "create.contraptions.roller_mode.straight_fill"
		)
		
		object WideFill : AdvancedRollingMode(
			icon = AllIcons.I_ROLLER_WIDE_FILL,
			translationKey = "create.contraptions.roller_mode.wide_fill"
		)
		
		object RailwayTrackReplace : AdvancedRollingMode(
			icon = CRIcons.I_SWAP_TRACKS,
			translationKey = "create.contraptions.roller_mode.track_replace",
		)
		
		object SmoothWideFill : AdvancedRollingMode(
			icon = AllIcons.I_ROLLER_WIDE_FILL,
			translationKey = "create.contraptions.roller_mode.smooth_wide_fill",
		)
		
		override fun getIcon(): AllIcons = icon
		override fun getTranslationKey(): String = translationKey
	}
	
	
	override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
		super.addBehaviours(behaviours)
		val scrollOptionIndex = behaviours.indexOfFirst { it is ScrollOptionBehaviour<*> }
		
		val previous = behaviours[scrollOptionIndex] as ScrollOptionBehaviour<*>
		val new = RollerScrollBehavior(
			previous.label,
			previous.blockEntity,
			previous.slotPositioning,
		)
		new.withCallback(this::onModeChanged)
		
		// As RollingMode is package-private
		val stub = StubScrollOptionBehaviour(
			new,
			previous.get().javaClass,
			previous.label,
			previous.blockEntity,
			previous.slotPositioning
		)
		setMode.invokeExact(this as RollerBlockEntity, stub as ScrollOptionBehaviour<*>)
		
		newMode = new
		behaviours[scrollOptionIndex] = new
	}
	
	override fun onModeChanged(mode: Int) {
		super.onModeChanged(mode)
		this.mode.value = mode
	}
	
	override fun isValidMaterial(newFilter: ItemStack): Boolean {
		val current = filtering.filter
		val currentItem = current.item
		if(currentItem is BlockItem) {
			val currentBlock = currentItem.block
			if(CompatMods.copycats) {
				if(currentBlock is ICopycatBlock) {
					val material = CopycatPaver.applyMaterialToItem(level!!, current, newFilter)
					if(material != null) {
						filtering.setFilter(material)
						return false
					}
				}
			}
		}
		
		val item = newFilter.item
		if(item is BlockItem) {
			val block = item.block
			if(block is GravelLayerBlock) return true
			
			if(CompatMods.copycats) {
				if(block is CopycatLayerBlock) return true
			}
		}
		
		return super.isValidMaterial(newFilter)
	}
	
	
	class RollerScrollBehavior(label: Component, be: SmartBlockEntity, slot: ValueBoxTransform) :
		ScrollOptionBehaviour<StubEnum>(StubEnum::class.java, label, be, slot), ScrollValueBehaviorExtension {
		
		val options: List<AdvancedRollingMode> = AdvancedRollingMode.Options
		
		val option: AdvancedRollingMode
			get() = AdvancedRollingMode.Options[value]
		
		
		init {
			between(0, options.size - 1)
		}
		
		override fun get(): Nothing =
			throw UnsupportedOperationException()
		
		override fun createValueBox(bb: AABB, pos: BlockPos): ValueBox =
			ValueBox.IconValueBox(label, option, bb, pos)
		
		override fun createBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard = ValueSettingsBoard(
			label,
			max,
			1,
			listOf(Component.literal("Select")),
			ValueSettingsFormatter.ScrollOptionSettingsFormatter(options.toTypedArray()),
		)
		
		override fun getClipboardKey(): String =
			"railx:AdvancedRollingMode"
	}
	
	enum class StubEnum : INamedIconOptions {}
}


private class StubScrollOptionBehaviour<E>(
	val new: ScrollOptionBehaviour<*>,
	enum: Class<E>,
	label: Component,
	be: SmartBlockEntity,
	slot: ValueBoxTransform,
) : ScrollOptionBehaviour<E>(enum, label, be, slot) where E : Enum<E>, E : INamedIconOptions {
	override fun getValue(): Int = new.value
	
	override fun setValue(value: Int) {
		new.value = value
	}
}
