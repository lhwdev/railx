package com.lhwdev.minecraft.railx.registry.key

fun interface KeyEventListener {
	fun onUpdate(key: AllKeys)
	
	fun interface Down {
		fun onDown(key: AllKeys)
	}
	
	fun interface Up {
		fun onUp(key: AllKeys)
	}
	
	
}
