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
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent.Context
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel


enum class AllPackets(val base: RailXPacketType<*>) {
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
	
	
	companion object {
		private val channelName = RailX.asResource("main")
		private const val networkVersion = "0"
		
		private var _channel: SimpleChannel? = null
		private var packetIndex = 0
		
		fun register() {
			val channel = NetworkRegistry.ChannelBuilder.named(channelName)
				.serverAcceptedVersions { it == networkVersion }
				.clientAcceptedVersions { it == networkVersion }
				.networkProtocolVersion { networkVersion }
				.simpleChannel()
			_channel = channel
			
			for(packet in entries) {
				val type = @Suppress("UNCHECKED_CAST") (packet.base as RailXPacketType<BasePacket>)
				channel.messageBuilder(type.typeClass, packetIndex++, type.direction)
					.encoder { packet, buffer -> packet.write(buffer) }
					.decoder { buffer -> type.read(buffer) }
					.consumerMainThread { packet, contextSupplier ->
						val context = contextSupplier.get()
						packet.handle(context)
						context.packetHandled = true
					}
					.add()
			}
		}
		
		
		val channel: SimpleChannel
			get() = _channel!!
		
		fun sendToPlayer(player: ServerPlayer, packet: ClientboundPacket) {
			channel.send(PacketDistributor.PLAYER.with { player }, packet)
		}
		
		fun sendToAllPlayers(packet: ClientboundPacket) {
			channel.send(PacketDistributor.ALL.noArg(), packet)
		}
		
		fun sendToServer(packet: ServerboundPacket) {
			channel.sendToServer(packet)
		}
	}
}


abstract class RailXPacketType<T : BasePacket> {
	val typeClass: Class<T> = calculateTypeClass()
	val name: String = calculateName()
	val direction: NetworkDirection = when {
		ServerboundPacket::class.java.isAssignableFrom(typeClass) -> NetworkDirection.PLAY_TO_SERVER
		ClientboundPacket::class.java.isAssignableFrom(typeClass) -> NetworkDirection.PLAY_TO_CLIENT
		else -> error("neither ServerboundPacket nor ClientboundPacket")
	}
	
	abstract fun read(buffer: FriendlyByteBuf): T
	
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
}


interface BasePacket {
	fun write(buffer: FriendlyByteBuf)
	
	fun handle(context: Context)
}

interface ServerboundPacket : BasePacket

abstract class ServerboundPacketBase : ServerboundPacket {
	abstract fun handle(player: ServerPlayer?)
	
	override fun handle(context: Context) {
		handle(context.sender)
	}
}

interface ClientboundPacket : BasePacket

abstract class ClientboundPacketBase : ClientboundPacket {
	abstract fun handle(player: LocalPlayer?)
	
	override fun handle(context: Context) {
		handle(Minecraft.getInstance().player)
	}
}
