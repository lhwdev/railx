package com.lhwdev.minecraft.railx.lua.asm

import com.lhwdev.minecraft.railx.cc.ObjectMetadataLayer

interface ObjectMetadata {
	val value: Any
	
	val knownFields: List<FieldMetadata>
	
	val staticProperties: List<PropertyMetadata>
	
	val staticMethods: List<MethodMetadata>
	
	val metatable: ObjectMetadata?
	
	fun findDynamicItem(name: String): ItemMetadata?
	
	val parents: List<ObjectMetadataLayer>
}
