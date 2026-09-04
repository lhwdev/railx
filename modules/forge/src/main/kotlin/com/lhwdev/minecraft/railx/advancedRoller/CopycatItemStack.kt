package com.lhwdev.minecraft.railx.advancedRoller

import com.copycatsplus.copycats.CCBlocks
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState


abstract class CopycatItemStack {
	abstract var materials: List<Material>
	
	abstract operator fun get(key: String): Material
	
	fun writeTo(registries: HolderLookup.Provider, stack: ItemStack) {
		val tag = stack.blockEntityData ?: CompoundTag()
		
		if("id" !in tag) {
			val block = (stack.item as? BlockItem)?.block as? IBE<*>
			if(block != null)
				tag.putString("id", BlockEntityType.getKey(block.blockEntityType)!!.toString())
		}
		
		writeTo(registries, tag)
		stack.blockEntityData = tag
	}
	
	fun writeTo(level: Level, stack: ItemStack) {
		writeTo(registries = level.registryAccess(), stack)
	}
	
	protected abstract fun writeTo(registries: HolderLookup.Provider, tag: CompoundTag)
	
	
	data class Material(val material: BlockState, val consumedItem: ItemStack, val enableCT: Boolean) {
		val isEmpty: Boolean
			get() = consumedItem.isEmpty
		
		companion object {
			val Empty: Material
				get() = Material(
					material = CCBlocks.COPYCAT_BASE.get().defaultBlockState(),
					consumedItem = ItemStack.EMPTY,
					enableCT = true
				)
			
			fun from(materialItem: MaterialItemStorage.MaterialItem): Material = Material(
				material = materialItem.material(),
				consumedItem = materialItem.consumedItem(),
				enableCT = materialItem.enableCT()
			)
			
			fun parse(registries: HolderLookup.Provider, tag: CompoundTag): Material {
				val enableCT = if(tag.contains(EnableCTKey)) tag.getBoolean(EnableCTKey) else true
				return if(tag.contains(MaterialKey)) {
					val blockGetter = registries.lookupOrThrow(Registries.BLOCK)
					val material = NbtUtils.readBlockState(blockGetter, tag.getCompound(MaterialKey))
					val consumedItem = ItemStack.parseOptional(registries, tag.getCompound(ItemKey))
					Material(material, consumedItem, enableCT)
				} else {
					Material(
						material = Blocks.AIR.defaultBlockState(),
						consumedItem = ItemStack.EMPTY,
						enableCT = enableCT
					)
				}
			}
		}
	}
	
	
	companion object {
		private const val MaterialKey = "Material"
		private const val ItemKey = "Item"
		private const val EnableCTKey = "EnableCT"
		private const val MultipartMaterialKey = "material_data"
		
		fun parse(registries: HolderLookup.Provider, stack: ItemStack): CopycatItemStack? {
			val item = stack.item as? BlockItem ?: return null
			val block = item.block as? ICopycatBlock ?: return null
			
			return if(block is IMultiStateCopycatBlock) {
				MultipartMaterial.parse(registries, stack)
			} else {
				SingleMaterial.parse(registries, stack)
			}
		}
		
		fun parse(level: Level, stack: ItemStack): CopycatItemStack? =
			parse(registries = level.registryAccess(), stack)
	}
	
	
	class SingleMaterial(var material: Material) : CopycatItemStack() {
		override var materials: List<Material>
			get() = listOf(material)
			set(value) {
				material = value.single()
			}
		
		override fun get(key: String): Material =
			material
		
		override fun writeTo(registries: HolderLookup.Provider, tag: CompoundTag) {
			val material = material
			
			val itemTag = com.copycatsplus.copycats.utility.ItemUtils.serializeNBT(material.consumedItem, registries)
			tag.put(ItemKey, itemTag)
			tag.put(MaterialKey, NbtUtils.writeBlockState(material.material))
			if(!material.enableCT) tag.putBoolean(EnableCTKey, false)
		}
		
		companion object {
			fun parse(registries: HolderLookup.Provider, stack: ItemStack): SingleMaterial {
				val tag = stack.blockEntityData ?: return SingleMaterial(material = Material.Empty)
				return SingleMaterial(material = Material.parse(registries, tag))
			}
		}
	}
	
	class MultipartMaterial(val storage: MaterialItemStorage) : CopycatItemStack() {
		private val keys = storage.allProperties.toList()
		
		override var materials: List<Material>
			get() = keys.map { Material.from(materialItem = storage.getMaterialItem(it)!!) }
			set(value) {
				require(value.size == keys.size)
				for((index, material) in value.withIndex()) {
					val key = keys[index]
					val item = storage.getMaterialItem(key)!!
					item.setMaterial(material.material)
					item.setConsumedItem(material.consumedItem)
					item.setEnableCT(material.enableCT)
				}
			}
		
		override fun get(key: String): Material =
			Material.from(materialItem = storage.getMaterialItem(key)!!)
		
		override fun writeTo(registries: HolderLookup.Provider, tag: CompoundTag) {
			val storageTag = storage.serialize(registries)
			tag.put(MultipartMaterialKey, storageTag)
		}
		
		companion object {
			fun parse(registries: HolderLookup.Provider, stack: ItemStack): MultipartMaterial? {
				val item = stack.item as? BlockItem ?: return null
				val block = item.block as? IMultiStateCopycatBlock ?: return null
				val tag = stack.blockEntityData
				val storage = MaterialItemStorage.create(block.storageProperties())
				if(tag != null) {
					storage.deserialize(tag.getCompound(MultipartMaterialKey), registries)
				}
				return MultipartMaterial(storage)
			}
		}
	}
}

var ItemStack.blockEntityData: CompoundTag?
	@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNNECESSARY_SAFE_CALL")
	get() = getOrDefault(DataComponents.BLOCK_ENTITY_DATA, null)?.copyTag()
	set(value) {
		if(value != null) this[DataComponents.BLOCK_ENTITY_DATA] = CustomData.of(value)
		else remove(DataComponents.BLOCK_ENTITY_DATA)
	}
