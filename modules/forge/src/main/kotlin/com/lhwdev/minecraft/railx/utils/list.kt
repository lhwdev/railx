package com.lhwdev.minecraft.railx.utils

import kotlin.experimental.ExperimentalTypeInference


inline fun </* kotlin.internal.NoInfer */ reified T> List<*>.requireAllInstanceOf(): List<T> {
	for((index, item) in withIndex()) {
		if(item !is T) throw ClassCastException("item $item at $index is not ${T::class.simpleName}")
	}
	@Suppress("UNCHECKED_CAST")
	return this as List<T>
}

inline fun <reified T> List<*>.asAllInstanceOf(): List<T>? =
	@Suppress("UNCHECKED_CAST")
	if(this.all { it is T }) this as List<T> else null

fun <T> MutableList<T>.indexOfOrPut(value: T): Int {
	val index = indexOf(value)
	return if(index == -1) {
		if(add(value)) {
			size - 1
		} else {
			-1
		}
	} else index
}


@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("averageOfInt")
inline fun <T> Collection<T>.averageOf(selector: (T) -> Int): Int =
	sumOf(selector) / size

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("averageOfDouble")
inline fun <T> Collection<T>.averageOf(selector: (T) -> Double): Double =
	sumOf(selector) / size
