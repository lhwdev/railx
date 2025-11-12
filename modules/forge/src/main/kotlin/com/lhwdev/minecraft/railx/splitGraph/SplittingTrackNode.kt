package com.lhwdev.minecraft.railx.splitGraph

import com.lhwdev.minecraft.railx.registry.AllPackets
import com.lhwdev.minecraft.railx.registry.RailXPacketType
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.simibubi.create.CreateClient
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNode
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.ClientboundPacketPayload
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*


class SplittingTrackNode(
	val splitPos: BlockPos,
	val isFrom: Boolean,
	var otherGraph: UUID,
	location: TrackNodeLocation,
	netId: Int,
	normal: Vec3,
) : TrackNode(location, netId, normal) {
	fun data(): Data = Data(splitPos, isFrom, otherGraph)
	
	fun createIfDifferentInto(graph: TrackGraph): TrackNode {
		val previous = graph.locateNode(location)
		if(
			previous is SplittingTrackNode &&
			splitPos == previous.splitPos &&
			isFrom == previous.isFrom &&
			otherGraph == previous.otherGraph
		) return previous
		graph.addCreatedNode(this)
		return this
	}
	
	fun writeSplit(): CompoundTag = CompoundTag { tag ->
		tag.putLong("Pos", splitPos.asLong())
		tag.putBoolean("From", isFrom)
		tag.putUUID("OtherGraph", otherGraph)
	}
	
	class Data(val splitPos: BlockPos, val isFrom: Boolean, val otherGraph: UUID) {
		companion object {
			@JvmField
			val STREAM_CODEC = object : StreamCodec<FriendlyByteBuf, Data> {
				override fun decode(buffer: FriendlyByteBuf): Data = Data(
					splitPos = buffer.readBlockPos(),
					isFrom = buffer.readBoolean(),
					otherGraph = buffer.readUUID(),
				)
				
				override fun encode(buffer: FriendlyByteBuf, value: Data) {
					buffer.writeBlockPos(value.splitPos)
					buffer.writeBoolean(value.isFrom)
					buffer.writeUUID(value.otherGraph)
				}
			}
		}
	}
	
	companion object {
		@JvmStatic
		fun readSplit(node: TrackNode, tag: CompoundTag): SplittingTrackNode = SplittingTrackNode(
			splitPos = BlockPos.of(tag.getLong("Pos")),
			isFrom = tag.getBoolean("From"),
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
			PacketDistributor.sendToAllPlayers(it)
			packet = null
		}
	}
	
	fun nodeOtherGraphChanged(graph: TrackGraph, node: SplittingTrackNode) {
		val packet = packet ?: SplittingTrackNodeUpdatedPacket().also { packet = it }
		packet.entries += SplittingTrackNodeUpdatedPacket.Entry(
			graphId = graph.id,
			nodeId = node.netId,
			otherGraph = node.otherGraph
		)
	}
}

class SplittingTrackNodeUpdatedPacket(val entries: MutableList<Entry> = mutableListOf()) : ClientboundPacketPayload {
	class Entry(val graphId: UUID, val nodeId: Int, val otherGraph: UUID) {
		companion object {
			val streamCodec = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Entry::graphId,
				ByteBufCodecs.VAR_INT, Entry::nodeId,
				UUIDUtil.STREAM_CODEC, Entry::otherGraph,
				::Entry,
			)
		}
	}
	
	companion object : RailXPacketType<SplittingTrackNodeUpdatedPacket>() {
		override val streamCodec = StreamCodec.composite(
			Entry.streamCodec.apply(ByteBufCodecs.list()), SplittingTrackNodeUpdatedPacket::entries,
			::SplittingTrackNodeUpdatedPacket
		)
	}
	
	override fun handle(player: LocalPlayer?) {
		val manager = CreateClient.RAILWAYS
		for(entry in entries) {
			val graph = manager.trackNetworks[entry.graphId] ?: continue
			val node = graph.getNode(entry.nodeId) as? SplittingTrackNode ?: continue
			node.otherGraph = entry.otherGraph
		}
	}
	
	override fun getTypeProvider(): BasePacketPayload.PacketTypeProvider =
		AllPackets.SplittingTrackNodeUpdated
}
