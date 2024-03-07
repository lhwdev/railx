package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.signal

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.common.api.EventHandle
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track.LuaTrackGraph
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train.LuaTrain


@LuaObject
interface LuaSignalGroup {
	@LuaProperty
	val trackGraph: LuaTrackGraph
	
	@LuaProperty
	val entries: List<LuaSignalEntry>
	
	@LuaProperty
	val isReserved: Boolean
	
	@LuaProperty
	val reservation: Reservation?
	
	@LuaProperty
	val trainsInside: List<LuaTrain>
	
	
	@LuaFunction
	fun createHandle(id: String): Handle
	
	/**
	 * Receives signal change event.
	 *
	 * - `railx:signal_changed`: args = `{ signalGroup: LuaSignalGroup, triggeredBy: LuaTrain }`
	 */
	@LuaFunction
	fun subscribeChange(): EventHandle
	
	
	@LuaObject
	interface Reservation {
		@LuaProperty
		val by: LuaTrain
		
		@LuaFunction
		fun minimumTimeLeft(): Float
		
		@LuaFunction
		fun rawMinimumTimeLeft(): Float
	}
	
	
	@LuaObject
	interface Handle {
		@LuaField
		val id: String
		
		@LuaProperty
		val reservedBySelf: Boolean
		
		@LuaFunction
		fun reserve(): Boolean
		
		@LuaFunction
		fun free(): Boolean
	}
}
