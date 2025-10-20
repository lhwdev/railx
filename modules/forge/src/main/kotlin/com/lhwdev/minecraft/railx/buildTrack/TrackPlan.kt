package com.lhwdev.minecraft.railx.buildTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlacementInfo
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.railx.utils.getList
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.RegistryOps
import net.neoforged.neoforge.common.CommonHooks


abstract class TrackPlan {
	companion object {
		val CODEC: Codec<TrackPlan> = object : Codec<TrackPlan> {
			override fun <T : Any?> encode(input: TrackPlan, ops: DynamicOps<T>, prefix: T): DataResult<T> =
				if(ops is RegistryOps<*>) {
					CompoundTag.CODEC.encode(input.write(CommonHooks.extractLookupProvider(ops)), ops, prefix)
				} else DataResult.error { "Use RegistryOps for TrackPlan.CODEC" }
			
			override fun <T : Any?> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<TrackPlan, T>> =
				if(ops is RegistryOps<*>) CompoundTag.CODEC.decode(ops, input)
					.map { Pair.of(TrackPlanImpl.read(CommonHooks.extractLookupProvider(ops), it.first), it.second) }
				else DataResult.error { "Use RegistryOps for TrackPlan.CODEC" }
		}
		
		// only server -> client
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TrackPlanImpl> = StreamCodec.composite(
			ComponentSerialization.TRUSTED_STREAM_CODEC, TrackPlanImpl::name,
			ByteBufCodecs.BOOL, TrackPlanImpl::addPlacedTracks,
			TrackSegment.STREAM_CODEC.apply(ByteBufCodecs.list()), TrackPlanImpl::segments,
			::TrackPlanImpl,
		)
	}
	
	abstract val name: Component
	
	open val addPlacedTracks: Boolean
		get() = false
	
	abstract fun addSegment(from: FlexiPlacementInfo)
	
	abstract fun write(registries: HolderLookup.Provider): CompoundTag
}


class TrackPlanImpl(
	override var name: Component,
	override var addPlacedTracks: Boolean,
	val segments: MutableList<TrackSegment>,
) : TrackPlan() {
	override fun addSegment(from: FlexiPlacementInfo) {
		segments += FlexiTrackSegmentImpl(from)
	}
	
	override fun write(registries: HolderLookup.Provider): CompoundTag = CompoundTag { tag ->
		tag.put(
			"Name",
			ComponentSerialization.CODEC
				.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), name)
				.orThrow,
		)
		tag.putBoolean("AddPlacedTracks", addPlacedTracks)
		tag.put("Segments", segments.mapTo(ListTag()) { it.write(registries) })
	}
	
	
	companion object {
		fun read(registries: HolderLookup.Provider, tag: CompoundTag): TrackPlanImpl = TrackPlanImpl(
			name = ComponentSerialization.CODEC.parse(
				registries.createSerializationContext(NbtOps.INSTANCE),
				tag.get("Name")
			).orThrow,
			addPlacedTracks = tag.getBoolean("AddPlacedTracks"),
			segments = tag.getList("Segments") { t: CompoundTag -> TrackSegment.read(registries, t) }.toMutableList(),
		)
	}
}

open class DummyTrackPlan : TrackPlan() {
	override val name: Component
		get() = Component.literal("dummy")
	
	override fun addSegment(from: FlexiPlacementInfo) {}
	
	override fun write(registries: HolderLookup.Provider): CompoundTag = error("cannot be called")
	
	companion object Default : DummyTrackPlan()
}

class ClientDummyTrackPlan : DummyTrackPlan()
