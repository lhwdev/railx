package com.lhwdev.minecraft.railx.api.lua


interface DynamicLuaMap<K, out V> : Map<K, V>

interface MutableDynamicLuaMap<K, V> : MutableMap<K, V>, DynamicLuaMap<K, V>


fun <K, V> DynamicLuaMap(original: Map<K, V>): DynamicLuaMap<K, V> =
	object : DynamicLuaMap<K, V>, Map<K, V> by original {}

fun <K, V> MutableDynamicLuaMap(original: MutableMap<K, V>): MutableDynamicLuaMap<K, V> =
	object : MutableDynamicLuaMap<K, V>, MutableMap<K, V> by original {}
