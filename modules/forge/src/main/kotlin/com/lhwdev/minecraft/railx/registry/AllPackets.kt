package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.ObserverEditPacket
import net.createmod.catnip.net.base.BasePacketPayload
import net.createmod.catnip.net.base.CatnipPacketRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload


enum class AllPackets(val base: RailXPacket<*>) : BasePacketPayload.PacketTypeProvider {
	/// client -> server
	ObserverEdit(ObserverEditPacket);
	
	
	override fun <T : CustomPacketPayload> getType(): CustomPacketPayload.Type<T> =
		@Suppress("UNCHECKED_CAST")
		(base.type.type as CustomPacketPayload.Type<T>)
	
	companion object {
		fun register() {
			val registry = CatnipPacketRegistry(RailX.Id, 1)
			for(packet in entries) {
				registry.registerPacket(packet.base.type)
			}
			registry.registerAllPackets()
		}
	}
}


abstract class RailXPacket<T : BasePacketPayload> {
	abstract val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>
	
	open val name: String
		get() = typeClass.name
	
	open val typeClass: Class<T> = typeClassNoCache
	
	protected open val typeClassNoCache: Class<T>
		get() = @Suppress("UNCHECKED_CAST") (this::class.java.enclosingClass as Class<T>)
	
	val type = CatnipPacketRegistry.PacketType(
		CustomPacketPayload.Type(RailX.asResource(typeClass.name)),
		typeClass,
		streamCodec,
	)
}
