package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.utils.getList
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity
import com.simibubi.create.content.trains.track.TrackMaterial
import net.createmod.catnip.data.Couple
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.*
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3


class MiddleTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	FakeTrackBlockEntity(type, pos, state) {
	
	var connections: List<BezierConnection> = emptyList()
	
	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.put("Connections", connections.mapTo(ListTag()) { CompactBezierConnection.write(it, blockPos) })
	}
	
	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		connections = tag.getList("Connections") { t: CompoundTag ->
			CompactBezierConnection.read(t, blockPos)
				.also { require(it.primary) { "curve is not primary" } }
		}
	}
}


private object CompactBezierConnection {
	fun read(tag: CompoundTag, localTo: BlockPos): BezierConnection = BezierConnection(
		tag.get("P").toCouple { t: LongTag -> BlockPos.of(t.asLong).offset(localTo) },
		tag.get("S").toCouple { t: ListTag -> VecHelper.readNBT(t).add(Vec3.atLowerCornerOf(localTo)) },
		tag.get("A").toCouple { t: ListTag -> VecHelper.readNBT(t) },
		tag.get("N").toCouple { t: ListTag -> VecHelper.readNBT(t) },
		tag.getBoolean("F"),
		tag.getBoolean("G"),
		TrackMaterial.deserialize(tag.getString("M")),
	).also { bc ->
		if("S" in tag) bc.smoothing = tag.get("S").toCouple { t: IntTag -> t.asInt }
	}
	
	fun write(bc: BezierConnection, localTo: BlockPos): CompoundTag = CompoundTag().also { tag ->
		tag.put("P", bc.bePositions.toListTag { LongTag.valueOf(it.subtract(localTo).asLong()) })
		tag.put("S", bc.starts.toListTag { VecHelper.writeNBT(it.subtract(Vec3.atLowerCornerOf(localTo))) })
		tag.put("A", bc.axes.toListTag { VecHelper.writeNBT(it) })
		tag.put("N", bc.normals.toListTag { VecHelper.writeNBT(it) })
		tag.putBoolean("F", bc.isPrimary)
		tag.putBoolean("G", bc.hasGirder)
		tag.putString("M", bc.material.id.toString())
		bc.smoothing?.let { s -> tag.put("S", s.toListTag { IntTag.valueOf(it) }) }
	}
	
	
	private inline fun <reified T, R> Tag?.toCouple(block: (T) -> R): Couple<R> {
		this as ListTag
		@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		return Couple.create(block(this[0] as T), block(this[1] as T))
	}
	
	private inline fun <T> Couple<T>.toListTag(block: (T) -> Tag): ListTag = ListTag().also { tag ->
		tag += block(first)
		tag += block(second)
	}
}
