package com.lhwdev.minecraft.railx.advancedRoller

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import com.simibubi.create.content.logistics.filter.FilterItem
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.item.ItemHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType

object CopycatPaver {
	private const val MaterialKey = "Material"
	private const val ItemKey = "Item"
	private const val MultipartMaterialKey = "material_data"
	
	
	fun consumeCopycatMaterials(
		context: MovementContext,
		stack: ItemStack,
		tag: CompoundTag,
		simulate: Boolean,
	): Boolean {
		val level = context.world
		val item = stack.item as BlockItem
		val block = item.block
		if(block is IMultiStateCopycatBlock) {
			val storage = MaterialItemStorage.create(block.storageProperties())
			storage.deserialize(tag.getCompound(MultipartMaterialKey), level.registryAccess())
			for(material in storage.allMaterialItems) {
				val consumed = material.consumedItem()
				val extracted = ItemHelper.extract(
					context.contraption.storage.allItems,
					{ stack -> FilterItem.testDirect(consumed, stack, true) },
					1,
					simulate,
				)
				if(extracted.isEmpty) return false
			}
		} else {
			val consumedItem = ItemStack.parseOptional(level.registryAccess(), tag.getCompound(ItemKey))
			if(consumedItem.isEmpty) return true
			val extracted = ItemHelper.extract(
				context.contraption.storage.allItems,
				{ stack -> stack.`is`(consumedItem.item) },
				consumedItem.count,
				simulate,
			)
			if(extracted.isEmpty) return false
		}
		
		return true
	}
	
	fun applyMaterialToItem(level: Level, stack: ItemStack, materialStack: ItemStack): ItemStack? {
		val item = stack.item as? BlockItem ?: return null
		val block = item.block as? ICopycatBlock ?: return null
		val entityBlock = block as? IBE<*> ?: return null
		val tag = stack[DataComponents.BLOCK_ENTITY_DATA]?.copyTag()
			?: CompoundTag { tag ->
				tag.putString("id", BlockEntityType.getKey(entityBlock.blockEntityType)!!.toString())
			}
		
		val material = block.getAcceptedBlockState(level, BlockPos.ZERO, materialStack, Direction.UP)
			?: return null
		
		if(block is IMultiStateCopycatBlock) {
			val storage = MaterialItemStorage.create(block.storageProperties())
			storage.deserialize(tag.getCompound(MultipartMaterialKey), level.registryAccess())
			for(item in storage.allMaterialItems) {
				if(!item.consumedItem().isEmpty) continue
				item.setMaterial(material)
				item.setConsumedItem(materialStack)
				
				val storageTag = storage.serialize(level.registryAccess())
				tag.put(MultipartMaterialKey, storageTag)
				stack[DataComponents.BLOCK_ENTITY_DATA] = CustomData.of(tag)
				return stack
			}
			return null
		} else {
			tag.put(MaterialKey, NbtUtils.writeBlockState(material))
			tag.put(ItemKey, materialStack.copyWithCount(1).save(level.registryAccess()))
			stack[DataComponents.BLOCK_ENTITY_DATA] = CustomData.of(tag)
			return stack
		}
	}
}
