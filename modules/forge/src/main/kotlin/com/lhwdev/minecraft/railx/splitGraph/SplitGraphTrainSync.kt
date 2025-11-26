package com.lhwdev.minecraft.railx.splitGraph

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataSerializer
import java.util.*
import kotlin.jvm.optionals.getOrNull


object SplitGraphTrainSync {
	class MergedInfo(val graphs: List<UUID>) {
		companion object {
			@JvmField
			val SERIALIZER = object : EntityDataSerializer<Optional<MergedInfo>> {
				override fun write(buffer: FriendlyByteBuf, value: Optional<MergedInfo>) {
					val v = value.getOrNull()
					if(v == null) buffer.writeBoolean(false)
					else {
						buffer.writeBoolean(true)
						buffer.writeCollection(v.graphs, FriendlyByteBuf::writeUUID)
					}
				}
				
				override fun read(buffer: FriendlyByteBuf): Optional<MergedInfo> = if(buffer.readBoolean()) {
					MergedInfo(graphs = buffer.readList(FriendlyByteBuf::readUUID))
						.let { Optional.of(it) }
				} else Optional.empty()
				
				override fun copy(value: Optional<MergedInfo>): Optional<MergedInfo> = value
			}
		}
	}
}
