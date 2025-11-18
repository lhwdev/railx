@file:JvmName("TrainForSplitUtils")

package com.lhwdev.minecraft.railx.splitGraph

import com.lhwdev.minecraft.railx.utils.asNoNulls
import com.simibubi.create.Create
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.entity.TrainMigration
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackGraphLocation
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntArraySet
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
import it.unimi.dsi.fastutil.objects.ReferenceArraySet
import net.minecraft.world.level.Level


@Suppress("FunctionName")
interface TrainForSplit {
	fun `railx$replaceGraphPreserving`(from: TrackGraph?, to: TrackGraph?)
}


fun Train.replaceGraphPreserving(from: TrackGraph?, to: TrackGraph?) {
	(this as TrainForSplit).`railx$replaceGraphPreserving`(from, to)
}


object TrainSplitUtils {
	class MigrationResult(val graphs: Int2ObjectMap<TrackGraph>)
	
	fun Train.reattachToTracks(level: Level, migrationPoints: List<TrainMigration>): MigrationResult? {
		val manager = Create.RAILWAYS
		
		val indices = IntArraySet()
		for(point in migrationPoints) indices.add(point.graphIndex)
		
		val result = arrayOfNulls<TrackGraphLocation>(migrationPoints.size)
		val pointGraphs = Reference2ObjectArrayMap<TrackGraph, TrackGraphLocation>()
		val allGraphs = Int2ObjectArrayMap<TrackGraph>()
		for(graphIndex in indices.toIntArray()) {
			var graphs: ReferenceArraySet<TrackGraph>? = null
			var pending: Reference2ObjectArrayMap<TrackGraph, Int2ObjectArrayMap<TrackGraphLocation>>? =
				null
			
			for((index, migration) in migrationPoints.withIndex()) {
				if(migration.graphIndex != graphIndex) continue
				
				pointGraphs.clear()
				for(graph in manager.trackNetworks.values) {
					val location = migration.tryMigratingNodesInto(graph) ?: continue
					pointGraphs[graph] = location
				}
				
				if(pointGraphs.isEmpty()) for(graph in manager.trackNetworks.values) {
					val location = migration.tryMigratingTo(graph) ?: continue
					pointGraphs[graph] = location
				}
				
				if(pointGraphs.isEmpty()) return null
				
				if(graphs == null) graphs = ReferenceArraySet(pointGraphs.keys)
				else graphs.retainAll(pointGraphs.keys)
				
				when(graphs.size) {
					0 -> return null
					1 -> result[index] = pointGraphs[graphs.first()]!!
					else -> {
						if(pending == null) pending = Reference2ObjectArrayMap()
						for(graph in graphs) pending.getOrPut(graph) { Int2ObjectArrayMap() }
							.put(index, pointGraphs[graph])
					}
				}
			}
			
			val graph = graphs?.firstOrNull() ?: return null
			allGraphs.put(graphIndex, graph)
			pending?.get(graph)?.let {
				for(entry in it.int2ObjectEntrySet())
					result[entry.intKey] = entry.value
			}
		}
		
		val locations = (result.asNoNulls() ?: return null).let { array ->
			if(allGraphs.size > 1) MovingTravellingPoint.MigrateTo().also { it += array }
			else array.toMutableList()
		}
		forEachTravellingPoint { it.migrateTo(locations) }
		return MigrationResult(graphs = allGraphs)
	}
}
