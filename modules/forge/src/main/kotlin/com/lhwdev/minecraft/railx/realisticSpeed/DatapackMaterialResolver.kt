package com.lhwdev.minecraft.railx.realisticSpeed

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.lhwdev.minecraft.railx.RailX
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.tags.TagKey
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.neoforge.common.NeoForge
import net.minecraftforge.neoforge.event.AddReloadListenerEvent
import net.minecraftforge.neoforge.event.TagsUpdatedEvent


object DatapackMaterialResolver : BlockMaterialResolver {
	fun register() {
		NeoForge.EVENT_BUS.register(this)
	}
	
	
	private val Materials = hashMapOf<ResourceLocation, BlockMaterial>()
	private val MaterialsFromTag = hashMapOf<ResourceLocation, BlockMaterial>()
	
	private class TagMaterial(val tag: ResourceLocation, val priority: Int, val material: BlockMaterial)
	
	private val TagMaterials = mutableListOf<TagMaterial>()
	
	internal object MaterialsLoader : SimpleJsonResourceReloadListener(Gson(), "railx.realistic_speed/materials") {
		override fun apply(
			objects: Map<ResourceLocation, JsonElement>,
			resourceManager: ResourceManager,
			profiler: ProfilerFiller,
		): Unit = try {
			for((location, element) in objects) {
				when {
					element.isJsonArray -> element.asJsonArray.forEach { parse(location, it.asJsonObject) }
					element.isJsonObject -> parse(location, element.asJsonObject)
					else -> throw IllegalArgumentException("root json element material data pack should be array or object")
				}
			}
		} catch(e: Exception) {
			RailX.Logger.error(e)
		}
		
		private fun parse(origin: ResourceLocation, element: JsonObject) {
			val priority = element["priority"]?.asInt ?: when {
				origin.namespace == RailX.Id -> 1000
				else -> 100
			}
			val mass = element["mass"]?.asDouble ?: throw IllegalArgumentException("no mass in $origin")
			// val friction = element["friction"]?.asDouble ?: 0.5
			
			val tag = element["tag"]?.asString
			val material = BlockMaterial(priority, mass, debugSource = tag ?: element["block"]?.asString)
			if(tag != null) {
				TagMaterials += TagMaterial(tag = ResourceLocation.parse(tag), priority, material)
			} else {
				val block = element["block"]?.asString ?: throw IllegalArgumentException("no 'tag' or 'block' key")
				val id = ResourceLocation.parse(block)
				val previous = Materials[id]
				if(previous == null || previous.priority < priority)
					Materials[id] = material
			}
		}
	}
	
	override val priority: Int
		get() = 1000
	
	override fun resolve(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial? {
		val block = BuiltInRegistries.BLOCK.getKey(state.block)
		Materials[block]?.let { return it }
		MaterialsFromTag[block]?.let { return it }
		return null
	}
	
	
	@SubscribeEvent
	private fun registerResourceManagers(event: AddReloadListenerEvent) {
		event.addListener(MaterialsLoader)
	}
	
	@SubscribeEvent
	private fun onTagsUpdated(@Suppress("unused") event: TagsUpdatedEvent) {
		MaterialsFromTag.clear()
		for(item in TagMaterials) {
			val tag = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, item.tag))
				?: continue
			if(!tag.isPresent) {
				RailX.Logger.warn("RealisticTrainSpeed.MaterialsLoader: cannot find tag ${item.tag}")
				continue
			}
			for(block in tag.get()) {
				MaterialsFromTag[block.key!!.location()] = item.material
			}
		}
	}
}
