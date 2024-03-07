package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.Registries
import com.lhwdev.minecraft.railx.api.blockEntity.PeripheralBehavior
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.contraptions.ITransformableBlockEntity
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.graph.TrackGraphHelper
import com.simibubi.create.content.trains.graph.TrackGraphLocation
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional

class TrainNetworkObserverBlockEntity(pos: BlockPos, state: BlockState) :
	SmartBlockEntity(Registries.TRAIN_NETWORK_OBSERVER_BLOCK_ENTITY.get(), pos, state), ITransformableBlockEntity {
	lateinit var edgePoint: TrackTargetingBehaviour<TrainNetworkObserver>
	
	val graphLocation: TrackGraphLocation?
		get() {
			val edgePoint = edgePoint
			val level = level!!
			
			return if(edgePoint.targetBezier != null) {
				TrackGraphHelper.getBezierGraphLocationAt(
					getLevel(),
					edgePoint.globalPosition,
					Direction.AxisDirection.POSITIVE,
					edgePoint.targetBezier
				)
			} else {
				val trackAxes =
					AllBlocks.TRACK.get().getTrackAxes(level, edgePoint.globalPosition, edgePoint.trackBlockState)
				if(trackAxes.size != 1) {
					level.destroyBlock(blockPos, true)
					return null
				}
				TrackGraphHelper.getGraphLocationAt(
					level,
					edgePoint.globalPosition,
					Direction.AxisDirection.POSITIVE,
					trackAxes[0]
				)
			}
		}
	
	private val peripheralBehavior = object : PeripheralBehavior<TrainNetworkObserverPeripheral>() {
		override fun createPeripheral() =
			TrainNetworkObserverPeripheral(this@TrainNetworkObserverBlockEntity)
		
		override fun willProvidePeripheralToSide(side: Direction) = true
	}
	
	override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> =
		peripheralBehavior.getCapability(cap, side) ?: super.getCapability(cap, side)
	
	override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
		behaviours.add(TrackTargetingBehaviour(this, NETWORK_OBSERVER).also { edgePoint = it })
	}
	
	override fun transform(structureTransform: StructureTransform) {
		edgePoint.transform(structureTransform)
	}
	
	companion object {
		val NETWORK_OBSERVER = EdgePointType.register(
			ResourceLocation(
				RailX.modId,
				"network_observer"
			)
		) { TrainNetworkObserver() }
	}
}
