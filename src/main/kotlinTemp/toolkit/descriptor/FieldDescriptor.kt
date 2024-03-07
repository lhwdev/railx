package com.lhwdev.asm.toolkit.descriptor

import com.lhwdev.asm.toolkit.Type
import java.lang.reflect.Field
import java.lang.reflect.Modifier


interface FieldDescriptor {
	enum class Kind { Instance, Static }
	
	
	val kind: Kind
	
	val owner: ClassDescriptor
	
	val name: String
	
	val fieldType: String
}

fun descriptor(field: Field): FieldDescriptor = SimpleFieldDescriptor(
	kind = when {
		Modifier.isStatic(field.modifiers) -> FieldDescriptor.Kind.Static
		else -> FieldDescriptor.Kind.Instance
	},
	owner = descriptor(field.declaringClass),
	name = field.name,
	fieldType = Type.getDescriptor(field.type)
)


class SimpleFieldDescriptor(
	override val kind: FieldDescriptor.Kind,
	override val owner: ClassDescriptor,
	override val name: String,
	override val fieldType: String,
) : FieldDescriptor
