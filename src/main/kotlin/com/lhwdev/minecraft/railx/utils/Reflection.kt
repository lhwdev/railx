package com.lhwdev.minecraft.railx.utils

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method


private val lookup = MethodHandles.publicLookup()

val <T : AccessibleObject> T.acc: T
	get() = this.also { it.isAccessible = true }

val Method.accHandle: MethodHandle
	get() = lookup.unreflect(acc)

val Field.accGetterHandle: MethodHandle
	get() = lookup.unreflectGetter(acc)

val Field.accSetterHandle: MethodHandle
	get() = lookup.unreflectSetter(acc)
