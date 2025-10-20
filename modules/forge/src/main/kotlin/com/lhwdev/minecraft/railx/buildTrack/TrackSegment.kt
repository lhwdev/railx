@file:Suppress("ClassName")

package com.lhwdev.minecraft.railx.buildTrack

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlacementInfo
import com.lhwdev.minecraft.railx.flexiTrack.asKnownSigned
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.railx.utils.closeTo
import com.lhwdev.minecraft.railx.utils.getVec3OrNull
import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.Create
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackMaterial
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NumericTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.times


abstract class TrackSegment {
	abstract val kind: Kind<*>
	
	abstract val from: End
	
	abstract val to: End
	
	abstract val material: TrackMaterial
	
	abstract val requiredItem: ItemStack
	
	abstract val curve: BezierConnection?
	
	
	protected abstract fun write(registries: HolderLookup.Provider, tag: CompoundTag)
	
	fun write(registries: HolderLookup.Provider): CompoundTag = CompoundTag { tag ->
		tag.putString("Kind", kind.id.toString())
		write(registries, tag)
	}
	
	object STREAM_CODEC : StreamCodec<RegistryFriendlyByteBuf, TrackSegment> {
		override fun decode(buffer: RegistryFriendlyByteBuf): TrackSegment {
			val type = buffer.readVarInt()
			return kindsByIndex[type].streamCodec.decode(buffer)
		}
		
		override fun encode(buffer: RegistryFriendlyByteBuf, value: TrackSegment) {
			buffer.writeVarInt(kindsByIndex.indexOf(value.kind))
			@Suppress("UNCHECKED_CAST")
			(value.kind.streamCodec as StreamCodec<RegistryFriendlyByteBuf, TrackSegment>).encode(buffer, value)
		}
	}
	
	companion object {
		private val kinds: MutableMap<ResourceLocation, Kind<*>> = mutableMapOf(
			Create.asResource("track") to CreateTrackSegmentImpl,
			RailX.asResource("flexi_track") to FlexiTrackSegmentImpl,
		)
		
		private val kindsByIndex = kinds.values.toMutableList()
		
		val Kinds: Map<ResourceLocation, Kind<*>>
			get() = kinds
		
		fun registerKind(kind: Kind<*>) {
			kinds[kind.id] = kind
			kindsByIndex += kind
		}
		
		fun read(registries: HolderLookup.Provider, tag: CompoundTag): TrackSegment {
			val kind = kinds.getValue(ResourceLocation.parse(tag.getString("Kind")))
			return kind.read(registries, tag)
		}
	}
	
	
	interface Kind<Segment : TrackSegment> {
		val id: ResourceLocation
		
		val streamCodec: StreamCodec<RegistryFriendlyByteBuf, Segment>
			get() = object : StreamCodec<RegistryFriendlyByteBuf, Segment> {
				override fun decode(buffer: RegistryFriendlyByteBuf): Segment =
					read(buffer.registryAccess(), buffer.readNbt()!!)
				
				override fun encode(buffer: RegistryFriendlyByteBuf, value: Segment) {
					buffer.writeNbt(value.write(buffer.registryAccess()))
				}
			}
		
		fun read(registries: HolderLookup.Provider, tag: CompoundTag): Segment
	}
	
	class End(val pos: BlockPos, val end: Vec3, val tangent: Vec3, val normal: Vec3) {
		fun write(): CompoundTag = CompoundTag { tag ->
			tag.putLong("Pos", pos.asLong())
			tag.put("T", tangent.asKnownSigned()?.let { IntTag.valueOf(it.index) } ?: VecHelper.writeNBT(tangent))
			if(!(normal.x similarTo 0.0) || !(normal.z similarTo 0.0)) tag.put("N", VecHelper.writeNBT(normal))
			if(!(pos.bottomCenter + tangent * 0.5 closeTo end)) tag.put("End", VecHelper.writeNBT(end))
		}
		
		companion object {
			fun read(tag: CompoundTag): End {
				val pos = BlockPos.of(tag.getLong("Pos"))
				val tangent = tag.get("T").let { t ->
					if(t is ListTag) {
						VecHelper.readNBT(t)
					} else {
						FlexiDirection.UnsignedKnown.readInt(t as NumericTag).tangent
					}
				}
				
				return End(
					pos = pos,
					end = tag.getVec3OrNull("End") ?: (pos.bottomCenter + tangent * 0.5),
					tangent = tangent,
					normal = tag.getVec3OrNull("N") ?: Vec3(0.0, 1.0, 0.0),
				)
			}
		}
	}
}


class CreateTrackSegmentImpl(val info: PlacementInfoAccessor) : TrackSegment() {
	companion object CreateKind : Kind<CreateTrackSegmentImpl> {
		override val id: ResourceLocation = Create.asResource("track")
		
		override fun read(registries: HolderLookup.Provider, tag: CompoundTag): CreateTrackSegmentImpl {
			TODO("Not yet implemented")
		}
	}
	
	override val kind: Kind<CreateTrackSegmentImpl>
		get() = CreateKind
	
	override val from: End = End(info.pos1, info.end1, info.axis1, info.normal1)
	override val to: End = End(info.pos2, info.end2, info.axis2, info.normal2)
	
	override val material: TrackMaterial
		get() = info.trackMaterial
	
	override val requiredItem: ItemStack = material.asStack(info.requiredTracks)
	
	override val curve: BezierConnection
		get() = info.curve
	
	override fun write(registries: HolderLookup.Provider, tag: CompoundTag) {
		TODO("Not yet implemented")
	}
}

class FlexiTrackSegmentImpl(val info: FlexiPlacementInfo) : TrackSegment() {
	companion object FlexiKind : Kind<FlexiTrackSegmentImpl> {
		override val id: ResourceLocation = RailX.asResource("flexi_track")
		
		override fun read(registries: HolderLookup.Provider, tag: CompoundTag): FlexiTrackSegmentImpl {
			val info = FlexiPlacementInfo.read(registries, tag.getCompound("FlexiPlacement"))
			return FlexiTrackSegmentImpl(info)
		}
	}
	
	override val kind: Kind<FlexiTrackSegmentImpl>
		get() = FlexiKind
	
	private fun end(from: FlexiPlacementInfo.TrackEnd) = End(from.pos, from.end, from.tangent, from.normal)
	
	override val from: End = end(info.from)
	override val to: End = end(info.to)
	
	override val material: TrackMaterial
		get() = info.material
	
	override val requiredItem: ItemStack
		get() = material.asStack(info.requiredTracks)
	
	override val curve: BezierConnection
		get() = info.curve
	
	override fun write(registries: HolderLookup.Provider, tag: CompoundTag) {
		tag.put("FlexiPlacement", info.write(registries))
	}
}
