package com.lhwdev.minecraft.railx.splitGraph

import com.simibubi.create.content.trains.entity.TrainMigration
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackGraphLocation


@Suppress("FunctionName")
interface TrainMigrationForSplit {
	fun `railx$getGraphIndex`(): Int
	fun `railx$setGraphIndex`(index: Int)
	
	fun `railx$tryMigratingNodesTo`(graph: TrackGraph): TrackGraphLocation?
}


var TrainMigration.graphIndex: Int
	get() = (this as TrainMigrationForSplit).`railx$getGraphIndex`()
	set(value) {
		(this as TrainMigrationForSplit).`railx$setGraphIndex`(value)
	}

fun TrainMigration.tryMigratingNodesInto(graph: TrackGraph): TrackGraphLocation? =
	(this as TrainMigrationForSplit).`railx$tryMigratingNodesTo`(graph)
