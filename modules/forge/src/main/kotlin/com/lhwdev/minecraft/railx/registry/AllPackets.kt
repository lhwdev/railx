package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.ObserverEditPacket
import com.lhwdev.minecraft.railx.compat.pantographsandwires.UpdateCantileverAutoPacket
import com.lhwdev.minecraft.railx.flexiTrack.FlexiblePlacementPacket
import com.lhwdev.minecraft.railx.middleTrack.CurvedMiddleTrackSelectionPacket
import com.lhwdev.minecraft.railx.realisticSpeed.control.UpdateRealisticPacket
import com.lhwdev.minecraft.railx.splitGraph.SplittingTrackNodeUpdatedPacket
import com.lhwdev.minecraft.railx.splitGraph.TrackGraphConnectedIdPacket
import com.lhwdev.minecraft.railx.throttle.StartControllingPacket
import com.lhwdev.minecraft.railx.throttle.ThrottlePacket
import com.lhwdev.minecraft.railx.throttle.UpdateThrottlePacket
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.CatnipPacketRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload


enum class AllPackets(val base: RailXPacketType<*>) : BasePacketPayload.PacketTypeProvider {
	/// client -> server
	FlexiblePlacement(FlexiblePlacementPacket),
	
	UpdateCantileverAuto(UpdateCantileverAutoPacket),
	
	UpdateRealistic(UpdateRealisticPacket),
	
	StartControlling(StartControllingPacket),
	UpdateThrottle(UpdateThrottlePacket),
	Throttle(ThrottlePacket),
	
	ObserverEdit(ObserverEditPacket),
	
	CurvedMiddleTrackSelection(CurvedMiddleTrackSelectionPacket),
	
	TrackGraphConnectedId(TrackGraphConnectedIdPacket),
	SplittingTrackNodeUpdated(SplittingTrackNodeUpdatedPacket),
	;
	
	
	override fun <T : CustomPacketPayload> getType(): CustomPacketPayload.Type<T> =
		@Suppress("UNCHECKED_CAST")
		(base.type as CustomPacketPayload.Type<T>)
	
	companion object {
		fun register() {
			val registry = CatnipPacketRegistry(RailX.Id, 1)
			for(packet in entries) {
				fun <T : BasePacketPayload> RailXPacketType<T>.catnipType() =
					CatnipPacketRegistry.PacketType<T>(type, typeClass, streamCodec)
				
				registry.registerPacket(packet.base.catnipType())
			}
			registry.registerAllPackets()
		}
	}
}


abstract class RailXPacketType<T : BasePacketPayload> {
	abstract val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>
	
	val typeClass: Class<T> = calculateTypeClass()
	val name: String = calculateName()
	
	protected open fun calculateTypeClass(): Class<T> =
		@Suppress("UNCHECKED_CAST") (this::class.java.enclosingClass as Class<T>)
	
	protected open fun calculateName(): String = typeClass.simpleName.let { name ->
		val length = if(name.endsWith("Packet")) name.length - 6 else name.length
		buildString {
			for(index in 0..<length) {
				val char = name[index]
				if(char.isUpperCase() && isNotEmpty()) {
					append('_')
				}
				append(char.lowercaseChar())
			}
		}
	}
	
	val type: CustomPacketPayload.Type<T> = CustomPacketPayload.Type(RailX.asResource(name))
}
