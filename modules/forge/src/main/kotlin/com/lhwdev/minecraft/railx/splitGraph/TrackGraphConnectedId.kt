@file:JvmName("TrackGraphConnectedIdUtils")

package com.lhwdev.minecraft.railx.splitGraph

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.ClientboundPacketBase
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.simibubi.create.Create
import com.simibubi.create.CreateClient
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.signal.SignalEdgeGroup
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.FriendlyByteBuf
import java.util.*


fun TrackGraph.updateConnectedIdForAllConnected(connectedId: UUID) {
	val manager = Create.RAILWAYS
	val visited = mutableSetOf<UUID>()
	val frontier = ArrayDeque<TrackGraph>().also { it += this }
	val packet = TrackGraphConnectedId.packetFor(connectedId)
	while(frontier.isNotEmpty()) {
		val current = frontier.removeFirst()
		if(!visited.add(current.id)) continue
		if(current.connectedId != connectedId) {
			current.connectedId = connectedId
			packet.graphs += current.id
		}
		
		current.connectedGraphs.mapNotNullTo(frontier) { manager.trackNetworks[it] }
	}
}

infix fun TrackGraph?.isReachableTo(other: TrackGraph?): Boolean {
	if(this == null) return false
	if(other == null) return false
	if(this == other) return true
	return connectedId?.let { it == other.connectedId } == true
}

infix fun Train.isReachableTo(other: Train): Boolean =
	graph isReachableTo other.graph


/**
 * All connected graphs have same connected id. So to say, when graph A and B are connected via splitting node, they
 * have same `connectedId`.
 */
object TrackGraphConnectedId {
	private var packet: TrackGraphConnectedIdPacket? = null
	
	
	internal fun onTick() {
		packet?.let {
			AllPackets.sendToAllPlayers(it)
			packet = null
		}
	}
	
	fun packetFor(connectedId: UUID?): TrackGraphConnectedIdPacket {
		packet?.let { packet ->
			if(packet.id == connectedId) return packet
			AllPackets.sendToAllPlayers(packet)
		}
		val packet = TrackGraphConnectedIdPacket(graphs = mutableListOf(), connectedId)
		this.packet = packet
		return packet
	}
	
	fun connectedIdChanged(graph: TrackGraph) {
		val connectedId = graph.connectedId
		val graphs = packetFor(connectedId).graphs
		if(graph.id !in graphs) graphs += graph.id
	}
	
	fun connectedGraphRemoved(graph: TrackGraph, @Suppress("unused") otherId: UUID) {
		val manager = Create.RAILWAYS
		val allConnected = graph.allConnectedGraphsIncludingSelf(manager)
		if(allConnected.size == 1) {
			graph.connectedId = null
			connectedIdChanged(graph)
			
			if(!manager.signalEdgeGroups.containsKey(graph.id)) {
				val group = SignalEdgeGroup(graph.id).asFallback()
				manager.signalEdgeGroups[graph.id] = group
				manager.sync.edgeGroupCreated(graph.id, group.color)
			}
			return
		}
		
		updateConnectedGraphs(graph, allConnected)
	}
	
	fun updateConnectedGraphs(
		graph: TrackGraph,
		allConnected: Set<TrackGraph> = graph.allConnectedGraphsIncludingSelf(Create.RAILWAYS),
	) {
		val previousMaster = Create.RAILWAYS.trackNetworks[graph.connectedId]
		if(previousMaster == null || previousMaster !in allConnected) {
			val master = allConnected.maxBy { it.nodes.size }
			val packet = packetFor(master.id)
			for(other in allConnected) {
				if(other.connectedId == master.id) continue
				other.connectedId = master.id
				packet.graphs += other.id
			}
		}
	}
	
	fun transferAll(from: TrackGraph, into: TrackGraph) {
		val manager = Create.RAILWAYS
		if(from.connectedId == from.id) {
			val allConnected = from.allConnectedGraphsIncludingSelf(manager)
			var master = allConnected.maxBy { it.nodes.size }
			if(master == from) master = into
			val packet = packetFor(master.id)
			into.connectedId = master.id
			packet.graphs += into.id
			
			for(other in allConnected) {
				other.connectedId = master.id
				packet.graphs += other.id
			}
		}
	}
}

class TrackGraphConnectedIdPacket(val graphs: MutableList<UUID>, val id: UUID?) : ClientboundPacketBase() {
	companion object : RailXPacketType<TrackGraphConnectedIdPacket>() {
		override fun read(buffer: FriendlyByteBuf): TrackGraphConnectedIdPacket = TrackGraphConnectedIdPacket(
			graphs = buffer.readList { it.readUUID() },
			id = if(buffer.readBoolean()) buffer.readUUID() else null,
		)
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeCollection(graphs, FriendlyByteBuf::writeUUID)
		buffer.writeNullable(id, FriendlyByteBuf::writeUUID)
	}
	
	override fun handle(player: LocalPlayer?) {
		val trackNetworks = CreateClient.RAILWAYS.trackNetworks
		for(graphId in graphs) {
			val graph = trackNetworks[graphId] ?: continue
			graph.connectedId = id
		}
	}
}
