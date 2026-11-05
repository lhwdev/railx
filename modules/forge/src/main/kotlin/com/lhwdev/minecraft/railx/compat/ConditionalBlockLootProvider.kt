package com.lhwdev.minecraft.railx.compat

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.resources.ResourceLocation
import java.nio.file.Path
import java.util.concurrent.CompletableFuture


/**
 * Adds following to loot table:
 * ```
 * "neoforge:conditions": [
 *     {
 *       "type": "neoforge:mod_loaded",
 *       "modid": "<inferred>"
 *     }
 *   ]
 * ```
 *
 * Where `modid` is provided by
 * [RailxRegistrate.registerCompat][com.lhwdev.minecraft.railx.registry.registrate.RailXRegistrate.registerCompat].
 */
class ConditionalBlockLootProvider(
	private val delegate: DataProvider,
	private val dataRoot: Path,
	private val blockDependencies: Map<ResourceLocation, String>,
) : DataProvider {
	override fun getName(): String =
		delegate.name
	
	override fun run(output: CachedOutput): CompletableFuture<*> = delegate.run { path, bytes, hash ->
		val mod = dependencyForLootTable(path)
		
		if(mod == null) {
			output.writeIfNeeded(path, bytes, hash)
		} else {
			val json = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
			val conditions = json.getAsJsonArray("neoforge:conditions") ?: JsonArray().also {
				json.add("neoforge:conditions", it)
			}
			
			val modLoadedCondition = JsonObject().apply {
				addProperty("type", "neoforge:mod_loaded")
				addProperty("modid", mod)
			}
			conditions.add(modLoadedCondition)
			
			DataProvider.saveStable(output, json, path).join()
		}
	}
	
	private fun dependencyForLootTable(path: Path): String? {
		if(!path.startsWith(dataRoot)) return null
		val relative = dataRoot.relativize(path).map { it.toString() }
		
		// loot table:`data/<namespace>/loot_table/blocks/<block path>.json`
		if(relative.size < 4) return null
		if(relative[1] != "loot_table" || relative[2] != "blocks" || !relative.last().endsWith(".json"))
			return null
		
		val namespace = relative[0]
		val blockPath = relative.drop(3).joinToString("/").removeSuffix(".json")
		return blockDependencies[ResourceLocation.fromNamespaceAndPath(namespace, blockPath)]
	}
}
