package com.lhwdev.minecraft.railx.advancedRoller

import com.copycatsplus.copycats.content.copycat.layer.CopycatLayerBlock
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock
import com.lhwdev.minecraft.railx.common.ScrollValueBehaviorExtension
import com.lhwdev.minecraft.railx.common.gravelLayer.GravelLayerBlock
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.railwayteam.railways.registry.CRIcons
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity
import com.simibubi.create.content.logistics.filter.FilterItem
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.*
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.item.ItemHelper
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.items.wrapper.InvWrapper
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
				add(FillMaterial)
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
			translationKey = "Smooth Wide Fill",
		)
		
		object FillMaterial : AdvancedRollingMode(
			icon = AllIcons.I_ROLLER_FILL,
			translationKey = "Fill Material",
		)
		
		override fun getIcon(): AllIcons = icon
		override fun getTranslationKey(): String = translationKey
	}
	
	
	override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
		super.addBehaviours(behaviours)
		
		/// Intercept FilteringBehavior
		val filteringIndex = behaviours.indexOfFirst { it is FilteringBehaviour }
		val previousFiltering = behaviours[filteringIndex] as FilteringBehaviour
		val filtering = RollerFilteringBehavior(this, previousFiltering.slotPositioning)
		filtering.label = previousFiltering.label
		filtering.withCallback(this::onFilterChanged)
		filtering.withPredicate(this::isValidMaterial)
		
		this.filtering = filtering
		behaviours[filteringIndex] = filtering
		
		/// Intercept ScrollOptionBehavior
		val scrollOptionIndex = behaviours.indexOfFirst { it is ScrollOptionBehaviour<*> }
		val previousScroll = behaviours[scrollOptionIndex] as ScrollOptionBehaviour<*>
		val newScroll = RollerScrollBehavior(
			previousScroll.label,
			previousScroll.blockEntity,
			previousScroll.slotPositioning,
		)
		newScroll.withCallback(this::onModeChanged)
		
		// As RollingMode is package-private
		val stubScroll = StubScrollOptionBehaviour(
			newScroll,
			previousScroll.get().javaClass,
			previousScroll.label,
			previousScroll.blockEntity,
			previousScroll.slotPositioning
		)
		setMode.invokeExact(this as RollerBlockEntity, stubScroll as ScrollOptionBehaviour<*>)
		
		newMode = newScroll
		behaviours[scrollOptionIndex] = newScroll
	}
	
	override fun onModeChanged(mode: Int) {
		super.onModeChanged(mode)
		this.mode.value = mode
	}
	
	override fun isValidMaterial(newFilter: ItemStack): Boolean {
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
	
	
	class RollerFilteringBehavior(be: SmartBlockEntity, slot: ValueBoxTransform) : FilteringBehaviour(be, slot) {
		override fun getTip(): MutableComponent {
			val currentStack = filter.item()
			val currentItem = currentStack.item
			if(currentItem is BlockItem) {
				val currentBlock = currentItem.block
				if(CompatMods.copycats) {
					if(currentBlock is ICopycatBlock) {
						val tip = Component.literal("Click with material to apply to copycat")
						val copycat = CopycatItemStack.parse(level = blockEntity.level!!, stack = currentStack)
						if(copycat != null && copycat.materials.any { !it.isEmpty }) {
							tip.append(", previously ")
							val materialsText = copycat.materials
								.filter { !it.isEmpty }
								.map { it.material.block.name }
								.fold(Component.empty()) { acc, material -> acc.append(material) }
							tip.append(materialsText)
						}
						return tip
					}
				}
			}
			
			return super.tip
		}
		
		private fun applyMaterial(newFilter: ItemStack): Boolean {
			val level = blockEntity.level!!
			val current = filter.item()
			val currentItem = current.item
			if(currentItem is BlockItem) {
				val currentBlock = currentItem.block
				if(CompatMods.copycats) {
					if(currentBlock is ICopycatBlock) {
						val state = currentBlock.getAcceptedBlockState(level, BlockPos.ZERO, newFilter, Direction.UP)
						val copycat = CopycatItemStack.parse(level = level, stack = current)
						if(copycat != null && state != null) {
							val material = copycat.materials.firstOrNull { it.isEmpty }
								?: copycat.materials.firstOrNull()
								?: return false
							val index = copycat.materials.indexOf(material)
							copycat.materials = copycat.materials.toMutableList().also { materials ->
								materials[index] = CopycatItemStack.Material(
									material = state,
									consumedItem = newFilter,
									enableCT = material.enableCT,
								)
							}
							copycat.writeTo(level, current)
							setFilter(current)
							return true
						}
					}
				}
			}
			
			return false
		}
		
		override fun setFilter(stack: ItemStack): Boolean {
			if(applyMaterial(stack)) {
				return false
			}
			return super.setFilter(stack)
		}
		
		override fun onShortInteract(
			player: Player,
			hand: InteractionHand,
			side: Direction,
			hitResult: BlockHitResult,
		) {
			val level = world
			val pos = pos
			val itemInHand = player.getItemInHand(hand)
			val toApply = itemInHand.copy()
			
			if(!canShortInteract(toApply)) return
			if(level.isClientSide) return
			
			if(getFilter(side).item is FilterItem) {
				val extracted = ItemHelper.extract(
					InvWrapper(player.inventory),
					{ stack -> ItemStack.isSameItemSameComponents(stack, getFilter(side)) },
					true
				)
				if(!player.isCreative || extracted.isEmpty)
					player.inventory.placeItemBackInInventory(getFilter(side).copy())
			}
			
			if(toApply.item is FilterItem) toApply.count = 1
			
			if(applyMaterial(toApply)) {
				player.displayClientMessage(
					Component.literal("Applied material ")
						.append(toApply.displayName)
						.append(" to ")
						.append(filter.item().displayName), true
				)
				return
			}
			
			if(!setFilter(side, toApply)) {
				player.displayClientMessage(CreateLang.translateDirect("logistics.filter.invalid_item"), true)
				AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1f, 1f)
				return
			}
			
			if(!player.isCreative) {
				if(toApply.item is FilterItem) {
					if(itemInHand.count == 1) player.setItemInHand(hand, ItemStack.EMPTY)
					else itemInHand.shrink(1)
				}
			}
			
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f)
		}
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
