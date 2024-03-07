package com.lhwdev.minecraft.railx.api.peripheral

import com.lhwdev.minecraft.railx.api.lua.LuaObjectProvidable
import dan200.computercraft.api.peripheral.IPeripheral


interface Peripheral : IPeripheral, LuaObjectProvidable {
	override fun provideObject(): Any = this
}
