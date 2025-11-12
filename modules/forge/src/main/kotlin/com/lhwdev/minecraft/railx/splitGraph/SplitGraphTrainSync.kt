package com.lhwdev.minecraft.railx.splitGraph

import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import java.util.*


object SplitGraphTrainSync {
	class MergedInfo(val graphs: List<UUID>) {
		companion object {
			val STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()),
				MergedInfo::graphs,
				::MergedInfo
			)
			
			val OPTIONAL_STREAM_CODEC =
				ByteBufCodecs.optional(STREAM_CODEC)
			
			@JvmField
			val SERIALIZER = EntityDataSerializer.forValueType(OPTIONAL_STREAM_CODEC)
		}
	}
}
