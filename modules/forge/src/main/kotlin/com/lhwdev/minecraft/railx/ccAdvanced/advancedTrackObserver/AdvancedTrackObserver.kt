package com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver

import com.lhwdev.minecraft.railx.RailX
import com.simibubi.create.Create
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.graph.DimensionPalette
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.observer.TrackObserver
import com.simibubi.create.content.trains.signal.SignalPropagator
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity


class AdvancedTrackObserver : TrackObserver() {
	companion object {
		val ObserverEdgePointType = EdgePointType.register(RailX.asResource("advanced_observer")) {
			AdvancedTrackObserver()
		}
	}
	
	
	var rule = AdvancedRule()
	
	
	override fun blockEntityAdded(blockEntity: BlockEntity, front: Boolean) {
		super.blockEntityAdded(blockEntity, front)
		rule = (blockEntity as AdvancedTrackObserverBlockEntity).rule
		notifyTrains(blockEntity.level!!)
	}
	
	private fun notifyTrains(level: Level) {
		val graph = Create.RAILWAYS.sided(level).getGraph(level, edgeLocation.getFirst()) ?: return
		val edge = graph.getConnection(edgeLocation.map { graph.locateNode(it) }) ?: return
		SignalPropagator.notifyTrains(graph, edge)
	}
	
	override fun keepAlive(train: Train) {
		
		super.keepAlive(train)
	}
	
	override fun read(
		nbt: CompoundTag,
		migration: Boolean,
		dimensions: DimensionPalette,
	) {
		super.read(nbt, migration, dimensions)
		rule.read(nbt.getCompound("Rule"))
	}
	
	override fun write(nbt: CompoundTag, dimensions: DimensionPalette) {
		super.write(nbt, dimensions)
		nbt.put("Rule", rule.write())
	}
}
