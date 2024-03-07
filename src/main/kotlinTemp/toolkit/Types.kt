package com.lhwdev.asm.toolkit


inline fun <reified T> type(): Type =
	Type.getType(T::class.java)
