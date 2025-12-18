package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.railx.utils.similarTo
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.data.Couple
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3


sealed interface FlexiPlaceResult {
	val valid: Boolean
	
	val error: PlaceError?
	
	
	open class PlaceError(val message: MutableComponent, val noOverlay: Boolean = false) :
		FlexiPlaceResult {
		override val valid: Boolean
			get() = false
		
		override val error: PlaceError
			get() = this
		
		fun noOverlay(): PlaceError = PlaceError(message, noOverlay = true)
		
		class SecondPoint : PlaceError(message = CreateLang.translateDirect("track.second_point"), noOverlay = true)
		class TooSharp : PlaceError(message = CreateLang.translateDirect("track.too_sharp"))
		class TooFar : PlaceError(message = CreateLang.translateDirect("track.too_far"))
	}
	
}

class FlexiPlacementInfo(
	val material: TrackMaterial,
	val trackItem: ItemStack,
	val from: TrackEnd,
	val to: TrackEnd,
) : FlexiPlaceResult {
	var addToPlan: Boolean = false
	var girder: Boolean = false
	var pavementBlock: Block? = null
	
	lateinit var curve: BezierConnection
	
	var requiredTracks: Int = 0
	var requiredPavement: Int = 0
	var hasRequiredTracks: Boolean = true
	var hasRequiredPavement: Boolean = true
	
	override var error: FlexiPlaceResult.PlaceError? = null
	override val valid: Boolean get() = error?.valid ?: true
	
	val minimumAllowedRadius: Int
		get() {
			var value = RailXConfig.Server.flexiTrak.minRadius.get()
			if(CompatMods.railways) when(material.trackType) {
				CRTrackMaterials.CRTrackType.WIDE_GAUGE -> value *= 2
				CRTrackMaterials.CRTrackType.NARROW_GAUGE, CRTrackMaterials.CRTrackType.UNIVERSAL -> value /= 2
			}
			return value
		}
	
	fun copy(): FlexiPlacementInfo {
		val info = FlexiPlacementInfo(material, trackItem, from, to)
		info.addToPlan = addToPlan
		info.girder = girder
		if(::curve.isInitialized) info.curve = curve
		info.pavementBlock = pavementBlock
		info.requiredTracks = requiredTracks
		info.requiredPavement = requiredPavement
		info.hasRequiredTracks = hasRequiredTracks
		info.hasRequiredPavement = hasRequiredPavement
		info.error = error
		return info
	}
	
	fun createCurve(): BezierConnection = BezierConnection(
		Couple.create(from.pos, to.pos),
		Couple.create(from.end, to.end),
		Couple.create(from.tangent, to.tangent),
		Couple.create(from.normal, to.normal),
		true,
		girder,
		material,
	)
	
	fun placeError(text: String) = placeError(Component.literal(text))
	fun placeErrorCreate(key: String) = placeError(CreateLang.translateDirect("track.$key"))
	fun placeError(text: MutableComponent) = placeError(FlexiPlaceResult.PlaceError(text))
	fun placeError(error: FlexiPlaceResult.PlaceError) = this.also { this.error = error }
	
	fun noOverlay(): FlexiPlacementInfo {
		error = error?.noOverlay()
		return this
	}
	
	
	data class TrackPoint(val pos: BlockPos, val tangent: Vec3, val normal: Vec3)
	
	data class TrackEnd(val state: BlockState, val pos: BlockPos, val end: Vec3, val tangent: Vec3, val normal: Vec3) {
		val block: ITrackBlock
			get() = state.block as ITrackBlock
		
		fun toPoint(): TrackPoint = TrackPoint(pos, tangent, normal)
		
		fun toKnownDirection(): FlexiDirection {
			if(normal.x similarTo 0.0 && normal.z similarTo 0.0) return FlexiDirection.Known.roundFrom(tangent)
			val normalized = FlexiDirection.Two(tangent, normal).toNormalized()
			return FlexiDirection.NormalizedImpl(FlexiDirection.Known.roundFrom(normalized.tangent), normalized.normal)
		}
		
		fun write(): CompoundTag = CompoundTag { tag ->
			tag.put("State", NbtUtils.writeBlockState(state))
			tag.put("Pos", NbtUtils.writeBlockPos(pos))
			tag.put("End", VecHelper.writeNBT(end))
			tag.put("Tangent", VecHelper.writeNBT(tangent))
			tag.put("Normal", VecHelper.writeNBT(normal))
		}
		
		companion object {
			fun read(tag: CompoundTag): TrackEnd = TrackEnd(
				state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("State")),
				pos = NbtUtils.readBlockPos(tag.getCompound("Pos")),
				end = VecHelper.readNBT(tag.get("End") as ListTag),
				tangent = VecHelper.readNBT(tag.get("Tangent") as ListTag),
				normal = VecHelper.readNBT(tag.get("Normal") as ListTag),
			)
		}
	}
	
	
	fun write(): CompoundTag = CompoundTag { tag ->
		if(error != null) throw IllegalStateException("cannot serialize info with error; error=$error")
		tag.putString("Material", material.id.toString())
		tag.put("Item", trackItem.save(CompoundTag()))
		tag.put("From", from.write())
		tag.put("To", to.write())
		tag.putBoolean("Girder", girder)
		pavementBlock?.let { tag.putString("Pavement", BuiltInRegistries.BLOCK.getKey(it).toString()) }
	}
	
	companion object {
		fun read(tag: CompoundTag): FlexiPlacementInfo {
			val info = FlexiPlacementInfo(
				material = TrackMaterial.deserialize(tag.getString("Material")),
				trackItem = ItemStack.of(tag.getCompound("Item")),
				from = TrackEnd.read(tag.getCompound("From")),
				to = TrackEnd.read(tag.getCompound("To")),
			)
			info.girder = tag.getBoolean("Girder")
			if("Pavement" in tag)
				info.pavementBlock = BuiltInRegistries.BLOCK.get(ResourceLocation(tag.getString("Pavement")))
			
			return info
		}
	}
}
