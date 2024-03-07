@file:Suppress(
	"INVISIBLE_REFERENCE",
	"INVISIBLE_MEMBER",
	"INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER",
	"CANNOT_OVERRIDE_INVISIBLE_MEMBER",
	"INAPPLICABLE_LATEINIT_MODIFIER"
)

import kotlinx.coroutines.delay
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn


class Hi {
	companion object {
		@JvmStatic
		fun hello() {
		}
	}
	
	private lateinit var hi: Any
	
	fun hi() {
		hi.toString()
	}
}

fun nullableArgs(a: Int = 3, b: String = "ho") {

}

suspend fun hello() {
	delay(1000)
	val num = other()
	delay(num)
	delay(1000)
}

suspend fun other(): Long {
	return 300
}


fun hello_decompiled(var0: Continuation<Any?>): Any? {
	class Continuation_1(c: Continuation<Any?>) : kotlin.coroutines.jvm.internal.ContinuationImpl(c) {
		lateinit var result: Result<Any?>
		var label: Int = 0
		
		override fun invokeSuspend(result: Result<Any?>): Any? {
			this.result = result
			this.label = this.label or Int.MIN_VALUE
			return hello_decompiled(this)
		}
	}
	
	
	val cont = if(var0 is Continuation_1) {
		if(var0.label and Int.MIN_VALUE != 0) {
			var0.label -= Int.MIN_VALUE
		}
		var0
	} else {
		Continuation_1(var0)
	}
	val result = cont.result
	val label = cont.label
	
	var previous: Any? = null
	
	when(label) {
		0 -> {
			result.throwOnFailure()
			cont.label = 1
			if(suspend { delay(1000) }.startCoroutineUninterceptedOrReturn(cont) == COROUTINE_SUSPENDED) {
				return COROUTINE_SUSPENDED
			}
		}
		
		1 -> result.throwOnFailure()
		2 -> {
			result.throwOnFailure()
			previous = result
		}
		
		3 -> result.throwOnFailure()
		4 -> {
			result.throwOnFailure()
			return Unit
		}
		
		else -> throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
	}
	
	if(label <= 1) {
		cont.label = 2
		previous = suspend { other() }.startCoroutineUninterceptedOrReturn(cont)
		if(previous == COROUTINE_SUSPENDED) {
			return COROUTINE_SUSPENDED
		}
	}
	if(label <= 2) {
		val num = previous as Long
		cont.label = 3
		if(suspend { delay(num) }.startCoroutineUninterceptedOrReturn(cont) == COROUTINE_SUSPENDED) {
			return COROUTINE_SUSPENDED
		}
	}
	
	cont.label = 4
	if(suspend { delay(1000) }.startCoroutineUninterceptedOrReturn(cont) == COROUTINE_SUSPENDED) {
		return COROUTINE_SUSPENDED
	}
	return Unit
}
