package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.peripherals.common.LuaGraphImpl


@LuaObject
class LuaTrackGraph : LuaGraphImpl<LuaTrackNode, LuaTrackEdge>(head = TODO()) {
	val blockGraph: Nothing
	
	override val delegate: Delegate<LuaTrackNode, LuaTrackEdge>
		get() = TODO("Not yet implemented")
}
