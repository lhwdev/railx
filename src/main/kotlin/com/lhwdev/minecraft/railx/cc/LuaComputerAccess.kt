package com.lhwdev.minecraft.railx.cc

import dan200.computercraft.api.peripheral.IComputerAccess


interface LuaComputerAccess : IComputerAccess {
	val machine: LuaMachineAccess
}
