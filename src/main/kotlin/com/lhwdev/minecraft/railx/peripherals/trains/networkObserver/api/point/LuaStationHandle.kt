package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.point

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train.LuaTrain


@LuaObject
interface LuaStationHandle {
	@LuaProperty
	val name: String
	
	@LuaProperty
	val presentTrain: LuaStationTrain?
	
	@LuaProperty
	val nearestTrain: LuaTrain?
	
	@LuaProperty
	val incomingTrain: LuaTrain?
}


@LuaObject
interface LuaStationTrain : LuaTrain {
	@LuaProperty
	val train: LuaTrain
	
	@LuaProperty
	val isAssembling: Boolean
	
	
	@LuaFunction
	fun disassemble()
	
	/**
	 * @param ownerUuid `"default"`: use previous uuid, or null; `"self"`: attach to this computer, so that can receive
	 * events related to train status.
	 */
	@LuaFunction
	fun assemble(ownerUuid: String? = "default")
}
