package com.lhwdev.minecraft.railx.buildTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlacementInfo
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.railx.utils.getList
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.util.ExtraCodecs


abstract class TrackPlan {
	companion object {
		val CODEC: Codec<TrackPlan> = object : Codec<TrackPlan> {
			override fun <T : Any?> encode(input: TrackPlan, ops: DynamicOps<T>, prefix: T): DataResult<T> =
				CompoundTag.CODEC.encode(input.write(), ops, prefix)
			
			override fun <T : Any?> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<TrackPlan, T>> =
				CompoundTag.CODEC.decode(ops, input)
					.map { Pair.of(TrackPlanImpl.read(it.first), it.second) }
		}
	}
	
	abstract val name: Component
	
	open val addPlacedTracks: Boolean
		get() = false
	
	abstract fun addSegment(from: FlexiPlacementInfo)
	
	abstract fun write(): CompoundTag
}


class TrackPlanImpl(
	override var name: Component,
	override var addPlacedTracks: Boolean,
	val segments: MutableList<TrackSegment>,
) : TrackPlan() {
	override fun addSegment(from: FlexiPlacementInfo) {
		segments += FlexiTrackSegmentImpl(from)
	}
	
	override fun write(): CompoundTag = CompoundTag { tag ->
		tag.put(
			"Name",
			ExtraCodecs.COMPONENT.encodeStart(NbtOps.INSTANCE, name).get().orThrow(),
		)
		tag.putBoolean("AddPlacedTracks", addPlacedTracks)
		tag.put("Segments", segments.mapTo(ListTag()) { it.write() })
	}
	
	
	companion object {
		fun read(tag: CompoundTag): TrackPlanImpl = TrackPlanImpl(
			name = ExtraCodecs.COMPONENT.parse(NbtOps.INSTANCE, tag.get("Name")).get().orThrow(),
			addPlacedTracks = tag.getBoolean("AddPlacedTracks"),
			segments = tag.getList("Segments") { t: CompoundTag -> TrackSegment.read(t) }.toMutableList(),
		)
	}
}

open class DummyTrackPlan : TrackPlan() {
	override val name: Component
		get() = Component.literal("dummy")
	
	override fun addSegment(from: FlexiPlacementInfo) {}
	
	override fun write(): CompoundTag = error("cannot be called")
	
	companion object Default : DummyTrackPlan()
}

class ClientDummyTrackPlan : DummyTrackPlan()
