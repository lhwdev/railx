package com.lhwdev.minecraft.railx.compat

import com.lhwdev.minecraft.railx.common.IBezierConnectionExtension
import com.simibubi.create.content.trains.track.BezierConnection
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnusedReceiverParameter", "unused")
fun BezierConnection.copiedFrom(from: BezierConnection) {
	// TODO: copies slab info from SnR
}

fun BezierConnection.onCurveUpdated() {
	BezierConnectionCompatReflection.getLazyRuntime(this).set(null)
	(this as IBezierConnectionExtension).onCurveUpdated()
}


private object BezierConnectionCompatReflection {
	val lazyRuntime = BezierConnection::class.java.getDeclaredField("lazyRuntime")
		.also { it.isAccessible = true }
		.let { MethodHandles.lookup().unreflectGetter(it) }
	
	fun getLazyRuntime(self: BezierConnection): AtomicReference<*> =
		lazyRuntime.invokeExact(self) as AtomicReference<*>
}
