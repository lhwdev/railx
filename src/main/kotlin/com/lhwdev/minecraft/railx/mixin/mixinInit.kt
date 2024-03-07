@file:Suppress("UnusedReceiverParameter")

package com.lhwdev.minecraft.railx.mixin

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.utils.accHandle
import org.spongepowered.asm.mixin.Mixin
import org.squiddev.cobalt.Lua


private object Dummy

fun RailX.mixinInit() {
	val thisModule = Dummy::class.java.module
	
	// See java/.../mixin/AccessHelper for more detail.
	val addReadsTo = Lua::class.java.getDeclaredMethod("railx\$addReadsTo", Module::class.java).accHandle
	addReadsTo.invoke(thisModule)
	addReadsTo.invoke(Mixin::class.java.module)
}
