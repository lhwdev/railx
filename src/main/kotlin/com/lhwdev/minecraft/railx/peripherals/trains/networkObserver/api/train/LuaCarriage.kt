package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.train

import com.lhwdev.minecraft.railx.api.lua.MutableDynamicLuaMap
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaField
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.signal.LuaSignalGroup
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.api.track.LuaTrackLocation


@LuaObject
interface LuaCarriage {
	@LuaField
	val id: Int
	
	@LuaProperty
	val bogeys: List<Bogey>
	
	@LuaProperty
	val metadata: MutableDynamicLuaMap<String, Any?>
	
	@LuaProperty
	val occupiedBlocks: List<LuaSignalGroup>
	
	@LuaProperty
	val headOccupiedPoint: LuaTrackLocation?
	
	@LuaProperty
	val tailOccupiedPoint: LuaTrackLocation?
	
	
	@LuaObject
	interface Bogey {
		// TODO
		val blockType: String
		
		val point: LuaTrackLocation?
	}
}
