package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train

import com.lhwdev.minecraft.railx.api.lua.DynamicLuaMap
import com.lhwdev.minecraft.railx.api.lua.RawLuaTable
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.common.LuaIndexedGraph
import com.lhwdev.minecraft.railx.peripherals.common.api.EventHandle
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.LuaUuid
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.point.LuaStationHandle
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.signal.LuaSignalGroup
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track.LuaTrackGraph
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track.LuaTrackNode
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track.LuaTrackPartialEdge


@LuaObject
interface LuaTrain : LuaTrainInfo {
	@LuaProperty
	val owner: LuaUuid?
	
	@LuaProperty
	val metadata: DynamicLuaMap<String, Any?>
	
	@LuaProperty
	val carriages: LuaIndexedGraph<LuaCarriage, LuaCarriageConnection>
	
	@LuaProperty
	val conductor: Conductor
	
	@LuaProperty
	val onTrack: OnTrack?
	
	@LuaObject
	interface OnTrack {
		@LuaProperty
		val graph: LuaTrackGraph
		
		@LuaField
		val motion: TrainMotion
		
		@LuaProperty
		val currentStation: LuaStationHandle?
		
		@LuaProperty
		val currentPath: List<LuaTrackPartialEdge>
		
		@LuaProperty
		val occupiedBlocks: List<LuaSignalGroup>
	}
	
	@LuaFunction
	fun subscribeChange(target: SubscribeTarget): EventHandle
	
	enum class SubscribeTarget { Location, OccupyingSignals, OccupyingEdges, Conductor }
	
	
	/// Sub-interfaces
	
	@LuaObject
	@LuaObject.SealedVariants(key = "type")
	sealed interface Conductor {
		@LuaObject
		interface Manual
		
		@LuaObject
		interface Computer
		
		@LuaObject
		interface Scheduled {
			@LuaProperty
			val frontConductor: Boolean
			
			@LuaProperty
			val backConductor: Boolean
		}
	}
	
	@LuaObject
	interface TrainMotion {
		@LuaProperty
		val speed: Double
		
		@LuaProperty
		val targetSpeed: Double
		
		
		/**
		 * Max acceleration for train to not slip.
		 * If acceleration is larger then [maxAcceleration], the train slips and fraction force may become smaller than
		 * static friction force.
		 */
		@LuaProperty
		val maxAcceleration: Double
		
		@LuaProperty
		var maxThrottle: Double
		
		@LuaProperty
		var maxBreaking: Double
		
		@LuaProperty
		var maxSpeed: Double
		
		@LuaFunction
		fun maxTurnSpeedMultiplier(maxCurvature: Double): Double
		
		
		@LuaProperty
		val isControlled: Boolean
		
		@LuaProperty
		val controlledBy: ControlSource?
		
		enum class ControlSource { Manual, Schedule, Computer }
		
		@LuaFunction
		fun control(): ManualHandle
		
		@LuaFunction
		fun approachSpeed(targetSpeed: Double)
		
		@LuaFunction
		fun startSchedule(schedule: Schedule)
		
		@LuaFunction
		fun startDynamicSchedule(scheduler: suspend (train: LuaTrain, navigation: Navigation) -> Unit)
		
		
		@LuaObject
		interface ManualHandle {
			@LuaProperty
			val isValid: Boolean
			
			@LuaField
			val throttle: Element
			
			@LuaField
			val `break`: Element
			
			@LuaField
			val emergencyBreak: Element
			
			
			@LuaFunction
			fun release()
			
			
			interface Element {
				/**
				 * From 0 to 1
				 */
				@LuaProperty
				var level: Double
				
				@LuaProperty
				val maxAcceleration: Double
			}
			
			enum class Steer { Left, Center, Right }
		}
		
		@LuaObject
		data class Schedule(
			val name: String,
			val movements: List<Movement>,
			val currentIndex: Int = 0,
		) {
			@LuaObject
			data class Movement(
				val from: Station,
				val to: Station,
				val condition: Condition,
			)
			
			@LuaObject
			@LuaObject.SealedVariants(key = "type")
			sealed class Condition {
				data class Daytime(val from: Int, val to: Int) : Condition()
				data class Wait(val ticks: Int) : Condition()
				data class Carriage(val item: String, val range: IntRange) : Condition()
				data class Or(val conditions: List<Condition>) : Condition()
				data class And(val conditions: List<Condition>) : Condition()
			}
			
			@LuaObject
			data class Station(val name: String) {
				companion object {
					@LuaObject.Deserializer
					fun deserialize(from: String) = Station(from)
					
					@LuaObject.Deserializer
					fun deserialize(from: RawLuaTable) = Station(from["name"].toString())
				}
			}
		}
		
		@LuaObject
		interface Navigation {
			@LuaProperty
			val currentNode: LuaTrackNode
			
			@LuaFunction
			suspend fun getNextBranch(): Branch
			
			@LuaFunction
			fun determineNext(branch: Branch, nextNode: LuaTrackNode)
			
			
			@LuaObject
			data class Branch(
				@LuaField
				val graph: LuaTrackGraph,
				
				@LuaField
				val node: LuaTrackNode,
				
				@LuaField
				val previousNode: LuaTrackNode,
				
				@LuaField
				val candidateEdges: List<LuaTrackNode>,
			)
		}
	}
}
