@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package com.lhwdev.minecraft.railx.throttle


object ThrottleStubs {
	class HeldControls(val throttle: Throttles.Throttle, controls: Collection<Int>) :
		Collection<Int> by throttleList(throttle, controls)
	
	private fun throttleList(throttle: Throttles.Throttle, controls: Collection<Int>) = buildList {
		if(throttle.gear >= 0) when(throttle.reverser) {
			Throttles.Reverser.Forward -> add(0)
			Throttles.Reverser.Neutral -> {}
			Throttles.Reverser.Backward -> add(1)
		}
		when(throttle.steering) {
			Throttles.Steering.Left -> add(2)
			Throttles.Steering.Neutral -> {}
			Throttles.Steering.Right -> add(3)
		}
		if(4 in controls) add(4)
		if(5 in controls) add(5)
	}
}
