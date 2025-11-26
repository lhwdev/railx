package com.lhwdev.minecraft.railx.splitGraph

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.ClientboundPacketBase
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.simibubi.create.CreateClient
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNode
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import net.minecraft.client.player.LocalPlayer
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.util.*


class SplittingTrackNode(
	var otherGraph: UUID,
	location: TrackNodeLocation,
	netId: Int,
	normal: Vec3,
) : TrackNode(location, netId, normal) {
	fun data(): Data = Data(otherGraph)
	
	fun createIfDifferentInto(graph: TrackGraph): TrackNode {
		val previous = graph.locateNode(location)
		if(
			previous is SplittingTrackNode &&
			otherGraph == previous.otherGraph
		) return previous
		graph.addCreatedNode(this)
		return this
	}
	
	fun writeSplit(): CompoundTag = CompoundTag { tag ->
		tag.putUUID("OtherGraph", otherGraph)
	}
	
	class Data(val otherGraph: UUID) {
		fun toNode(location: TrackNodeLocation, netId: Int, normal: Vec3): SplittingTrackNode = SplittingTrackNode(
			otherGraph = otherGraph,
			location = location,
			netId = netId,
			normal = normal,
		)
		
		fun write(buffer: FriendlyByteBuf) {
			buffer.writeUUID(otherGraph)
		}
		
		companion object {
			@JvmStatic
			fun read(buffer: FriendlyByteBuf): Data = Data(
				otherGraph = buffer.readUUID(),
			)
		}
	}
	
	companion object {
		@JvmStatic
		fun readSplit(node: TrackNode, tag: CompoundTag): SplittingTrackNode = SplittingTrackNode(
			otherGraph = tag.getUUID("OtherGraph"),
			location = node.location,
			netId = node.netId,
			normal = node.normal,
		)
	}
}


object SplittingTrackNodeSync {
	private var packet: SplittingTrackNodeUpdatedPacket? = null
	
	internal fun onTick() {
		packet?.let {
			AllPackets.sendToAllPlayers(it)
			packet = null
		}
	}
	
	fun nodeOtherGraphChanged(graph: TrackGraph, node: SplittingTrackNode) {
		val packet = packet ?: SplittingTrackNodeUpdatedPacket().also { packet = it }
		packet.entries += SplittingTrackNodeUpdatedPacket.Entry(
			graphId = graph.id,
			nodeId = node.netId,
			otherGraph = node.otherGraph,
		)
	}
}

class SplittingTrackNodeUpdatedPacket(val entries: MutableList<Entry> = mutableListOf()) : ClientboundPacketBase() {
	class Entry(val graphId: UUID, val nodeId: Int, val otherGraph: UUID) {
		fun write(buffer: FriendlyByteBuf) {
			buffer.writeUUID(graphId)
			buffer.writeVarInt(nodeId)
			buffer.writeUUID(otherGraph)
		}
		
		companion object {
			fun read(buffer: FriendlyByteBuf): Entry = Entry(
				graphId = buffer.readUUID(),
				nodeId = buffer.readVarInt(),
				otherGraph = buffer.readUUID(),
			)
		}
	}
	
	companion object : RailXPacketType<SplittingTrackNodeUpdatedPacket>() {
		override fun read(buffer: FriendlyByteBuf): SplittingTrackNodeUpdatedPacket = SplittingTrackNodeUpdatedPacket(
			entries = buffer.readList { buffer -> Entry.read(buffer) }
		)
	}
	
	override fun write(buffer: FriendlyByteBuf) {
		buffer.writeCollection(entries) { buffer, value -> value.write(buffer) }
	}
	
	override fun handle(player: LocalPlayer?) {
		val manager = CreateClient.RAILWAYS
		for(entry in entries) {
			val graph = manager.trackNetworks[entry.graphId] ?: continue
			val node = graph.getNode(entry.nodeId) as? SplittingTrackNode ?: continue
			node.otherGraph = entry.otherGraph
		}
	}
}
