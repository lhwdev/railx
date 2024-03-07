package com.lhwdev.minecraft.railx.cc

import com.lhwdev.minecraft.railx.api.lua.LuaArguments
import com.lhwdev.minecraft.railx.api.lua.LuaVararg
import com.lhwdev.minecraft.railx.api.lua.RawLuaTable
import com.lhwdev.minecraft.railx.api.lua.annotations.*
import com.lhwdev.minecraft.railx.lua.LuaMemberContext
import com.lhwdev.minecraft.railx.lua.asm.*
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.MethodResult
import org.squiddev.cobalt.LuaError
import org.squiddev.cobalt.LuaTable
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*


class LuaTableSupplierImpl : LuaTableSupplier {
	val lookup = MethodHandles.lookup()
	private val cache = mutableMapOf<KClass<*>, ObjectMetadataFactory>()
	
	override fun getObjectMetadata(value: Any): ObjectMetadata {
		val layer = getObjectMetadataLayer(value)
		return ObjectMetadataBuilder().apply { addLayer(layer) }.build(value)
	}
	
	fun getObjectMetadataLayer(value: Any): ObjectMetadataLayer {
		val clazz = value::class
		val staticMetadata = cache.getOrPut(clazz) {
			ObjectMetadataFactory(
				impl = this,
				clazz = clazz,
				parents = listOf(),
			)
		}
		
		return staticMetadata.toObjectMetadata(value)
	}
}


fun ObjectMetadataLayer.knownToMap(): Map<String, ItemMetadata> {
	val result = HashMap<String, ItemMetadata>()
	fun ObjectMetadataLayer.addRecursively() {
		fun putAll(items: List<ItemMetadata>) {
			items.forEach { result.putIfAbsent(it.name, it) }
		}
		putAll(knownFields)
		putAll(staticProperties)
		putAll(staticMethods)
		for(parent in parents) {
			parent.addRecursively()
		}
	}
	addRecursively()
	return result
}

private fun MethodHandle.coerceToSuspend(): MethodHandle =
	MethodHandles.dropArguments(this, type().parameterCount(), Continuation::class.java)

private fun KProperty1<*, *>.getValueHandle(lookup: MethodHandles.Lookup): MethodHandle {
	val handle = javaGetter?.let { lookup.unreflect(it) } ?: lookup.unreflectGetter(javaField!!)
	
	require(handle.type().parameterCount() == 1) { "expected $name getter to have arity 1" }
	
	return handle
}

private fun KMutableProperty1<*, *>.setValueHandle(lookup: MethodHandles.Lookup): MethodHandle {
	val handle = javaSetter?.let { lookup.unreflect(it) } ?: lookup.unreflectSetter(javaField!!)
	
	require(handle.type().parameterCount() == 2) { "expected $name getter to have arity 2" }
	
	return handle
}


interface DynamicExtra {
	fun findItem(name: String): ItemMetadata?
}

interface ObjectMetadataLayer {
	val knownFields: List<FieldMetadata> get() = emptyList()
	val staticProperties: List<PropertyMetadata> get() = emptyList()
	val staticMethods: List<MethodMetadata> get() = emptyList()
	val dynamicExtras: List<DynamicExtra> get() = emptyList()
	val metatable: ObjectMetadataLayer? get() = null
	val parents: List<ObjectMetadataLayer> get() = emptyList()
}

private class ObjectMetadataBuilder {
	val knownFields = mutableListOf<FieldMetadata>()
	val staticProperties = mutableListOf<PropertyMetadata>()
	val staticMethods = mutableListOf<MethodMetadata>()
	val dynamicExtras = mutableListOf<DynamicExtra>()
	val metatables = mutableListOf<ObjectMetadataLayer>()
	val parents = mutableListOf<ObjectMetadataLayer>()
	
	fun addLayer(layer: ObjectMetadataLayer) {
		knownFields += layer.knownFields
		staticProperties += layer.staticProperties
		staticMethods += layer.staticMethods
		dynamicExtras += layer.dynamicExtras
		layer.metatable?.let { metatables += it }
		parents += layer.parents
	}
	
	fun build(value: Any): ObjectMetadata {
		val builder = this
		return object : ObjectMetadata {
			override val value: Any = value
			override val knownFields = builder.knownFields
			override val staticProperties = builder.staticProperties
			override val staticMethods = builder.staticMethods
			override val metatable: ObjectMetadata? =
				if(metatables.isNotEmpty()) {
					ObjectMetadataBuilder().apply { metatables.forEach { addLayer(it) } }.build(value)
				} else {
					null
				}
			
			private val dynamicExtras = builder.dynamicExtras
			override fun findDynamicItem(name: String): ItemMetadata? {
				for(extra in dynamicExtras) {
					extra.findItem(name)?.let { return it }
				}
				return null
			}
			
			override val parents = builder.parents
			
			override fun toString(): String = """
				ObjectMetadata {
					knownFields = $knownFields
					staticProperties = $staticProperties
					staticMethods = $staticMethods
					metatable = $metatable
					dynamicExtras = $dynamicExtras
					parents = $parents
				}
			""".trimIndent()
		}
	}
}

private class ObjectMetadataFactory(
	impl: LuaTableSupplierImpl,
	clazz: KClass<*>,
	val parents: List<ObjectMetadataFactory>,
) {
	
	val knownFieldMappers: (Any) -> List<FieldMetadata>
	
	val staticProperties: List<PropertyMetadata>
	
	val staticMethods: List<MethodMetadata>
	
	val extraFactories: List<ExtraFactory>
	
	private val classCaches = KClassCaches()
	
	
	init {
		val knownFieldHandles = mutableListOf<FieldMetadataFactory>()
		val staticProperties = mutableListOf<PropertyMetadata>()
		val extraFactories = mutableListOf<StaticExtraFactory>()
		val staticMethods = mutableListOf<MethodMetadata>()
		
		// TODO: additional proxy methods for Iterable, Iterator, Collection, ...
		//       See http://lua-users.org/wiki/MetatableEvents
		
		// For Properties
		for(property in clazz.memberProperties) {
			if(property.visibility != KVisibility.PUBLIC) continue
			
			val fieldAnnotation = property.findAnnotationInherited<LuaField>(clazz)
			if(fieldAnnotation != null) {
				knownFieldHandles += FieldMetadataFactory(impl, property, fieldAnnotation)
				continue
			}
			
			val annotation = property.findAnnotationInherited<LuaProperty>(clazz)
			if(annotation != null) {
				staticProperties += PropertyMetadataImpl(impl, property, annotation)
				continue
			}
			
			val extraAnnotation = property.findAnnotationInherited<LuaExtra>(clazz)
			if(extraAnnotation != null) {
				val target = extraAnnotation.target
				val isDynamic = extraAnnotation.dynamic
				
				extraFactories += if(isDynamic) {
					TODO()
				} else {
					StaticExtraFactory(impl, target, property)
				}
				continue
			}
		}
		
		
		// For Member Functions
		for(function in clazz.memberFunctions) {
			if(function.visibility != KVisibility.PUBLIC) continue
			
			// val extraAnnotation = function.findAnnotation<LuaExtra>()
			// val getterAnnotation = function.findAnnotation<LuaGetter>()
			// val setterAnnotation = function.findAnnotation<LuaSetter>()
			val annotation = function.findAnnotationInherited<LuaFunction>(clazz)
			if(annotation != null) {
				staticMethods += MethodMetadataImpl(impl, function, annotation)
			}
		}
		
		
		this.knownFieldMappers = { self ->
			knownFieldHandles.map { it.withSelf(self) }
		}
		
		this.staticProperties = staticProperties
		this.staticMethods = staticMethods
		this.extraFactories = extraFactories
	}
	
	
	private inline fun <reified T : Annotation> KCallable<*>.findAnnotationInherited(clazz: KClass<*>) =
		findAnnotationInherited(clazz, T::class, classCaches)
	
	fun toObjectMetadata(self: Any): ObjectMetadataLayer {
		val factory = this
		val allExtras = factory.extraFactories
			.groupBy { it.target }
			.mapValues { (_, values) ->
				val extras = values.map {
					val extra = it.withSelf(self)
					val knownMap = extra.knownToMap()
					
					({ name: String ->
						knownMap[name] ?: extra.dynamicExtras.firstNotNullOfOrNull { it.findItem(name) }
					})
				}
				
				
				listOf(object : DynamicExtra {
					override fun findItem(name: String): ItemMetadata? =
						extras.firstNotNullOfOrNull { it(name) }
				})
			}
		
		return object : ObjectMetadataLayer {
			override val knownFields: List<FieldMetadata> = factory.knownFieldMappers(self)
			override val staticProperties: List<PropertyMetadata> = factory.staticProperties
			override val staticMethods: List<MethodMetadata> = factory.staticMethods
			override val dynamicExtras: List<DynamicExtra> = allExtras[LuaExtra.Target.Self] ?: emptyList()
			override val parents: List<ObjectMetadataLayer> =
				factory.parents.map { it.toObjectMetadata(self) }
		}
	}
}

sealed class ExtraFactory {
	abstract val target: LuaExtra.Target
	
	abstract fun withSelf(self: Any): ObjectMetadataLayer
}

class DynamicExtraFactory(
	impl: LuaTableSupplierImpl,
	override val target: LuaExtra.Target,
	property: KProperty1<*, *>,
) : ExtraFactory() {
	init {
		TODO()
	}
	
	private val handle = property.getValueHandle(impl.lookup)
	
	override fun withSelf(self: Any) = object : ObjectMetadataLayer {
		override val dynamicExtras: List<DynamicExtra>
			get() = TODO()
	}
}

class StaticExtraFactory(
	private val impl: LuaTableSupplierImpl,
	override val target: LuaExtra.Target,
	property: KProperty1<*, *>,
) : ExtraFactory() {
	private val handle = property.getValueHandle(impl.lookup)
	
	override fun withSelf(self: Any): ObjectMetadataLayer {
		val extra = handle.invoke(self)
		
		return when(extra) {
			is Map<*, *> -> object : ObjectMetadataLayer {
				override val dynamicExtras: List<DynamicExtra> = listOf(object : DynamicExtra {
					override fun findItem(name: String): ItemMetadata? =
						@Suppress("UNCHECKED_CAST")
						(FieldMetadata(
							name,
							(extra as Map<Any?, *>).getOrElse(name) { return null }))
					
					override fun toString(): String =
						"StaticExtra(target=$target, handle=$handle)"
				})
			}
			
			else -> impl.getObjectMetadataLayer(extra)
		}
	}
}


class FieldMetadataFactory(impl: LuaTableSupplierImpl, field: KProperty1<out Any, *>, annotation: LuaField) {
	val name: String = annotation.value.ifBlank { field.name }
	
	val handle: MethodHandle = field.getValueHandle(impl.lookup)
	
	fun withSelf(self: Any): FieldMetadata {
		val factory = this
		return object : FieldMetadata {
			override val name: String = factory.name
			override val value: Any? = factory.handle.invoke(self)
			
			override fun toString(): String =
				"FieldMetadata(name=$name, value=$value)"
		}
	}
}

class PropertyMetadataImpl(
	impl: LuaTableSupplierImpl,
	property: KProperty1<out Any, *>,
	annotation: LuaProperty,
) : PropertyMetadata {
	init {
		assert(annotation.type == LuaProperty.Type.Accessor)
	}
	
	override val name: String = annotation.value.ifBlank { property.name }
	
	private val getHandle = property.getValueHandle(impl.lookup)
	
	private val setHandle = if(property is KMutableProperty1 && !annotation.readonly) {
		property.setValueHandle(impl.lookup)
	} else null
	
	override val isMutable: Boolean
		get() = setHandle != null
	
	
	override suspend fun get(self: Any): Any? =
		getHandle.invoke(self)
	
	override suspend fun set(self: Any, value: Any?) {
		(setHandle ?: error("this property is readonly")).invoke(self, value)
	}
	
	override fun toString(): String =
		"PropertyMetadata(name=$name, getHandle=$getHandle, setHandle=$setHandle)"
}

private fun List<Class<*>>.pickArgumentHandle(index: Int): MethodHandle {
	var handle = MethodHandles.identity(this[index])
	handle = MethodHandles.dropArguments(handle, 1, this.drop(index + 1)) // add last arguments
	handle = MethodHandles.dropArguments(handle, 0, this.take(index)) // add first arguments
	return handle
}

class MethodMetadataImpl(
	impl: LuaTableSupplierImpl,
	function: KFunction<*>,
	annotation: LuaFunction,
) : MethodMetadata {
	companion object {
		// Helpers
		
		@Suppress("unused", "UNUSED_PARAMETER")
		@PublishedApi
		internal object Helper {
			@JvmStatic
			fun asNullable(value: Optional<Any?>): Any? = value.orElse(null)
			
			@JvmStatic
			fun getBackingObject(value: LuaTable): Any = value.getBackingObject() ?: error("value is not LuaObject")
			
			@JvmStatic
			fun bitFlagRaw(allArgs: Int, count: Int): Int {
				var result = 0
				for(i in count..<allArgs) {
					result = result or 1 shr i
				}
				return result
			}
			
			@JvmStatic
			fun defaultBitFlag(self: RawLuaTable, args: ArgumentsHelper, allArgs: Int): Int =
				bitFlagRaw(allArgs, args.size)
		}
		
		class ArgumentsHelper(val base: LuaArguments) {
			val size: Int get() = base.size
			
			fun get(index: Int) = base.getAny(index)
			fun getInt(index: Int) = base.getInt(index)
			fun getBoolean(index: Int) = base.getBoolean(index)
			fun getDouble(index: Int) = base.getDouble(index)
			fun getLong(index: Int) = base.getLong(index)
			fun getMap(index: Int) = base.getMap(index)
			fun getString(index: Int) = base.getString(index)
			fun getBytes(index: Int) = base.getBytes(index)
			fun getEnum(index: Int, klass: Class<out Enum<*>>) = base.getEnum(index, klass)!!
			
			
			fun getNullable(index: Int) = if(base.getNullable(index)) base.getAny(index) else null
			fun getNullableInt(index: Int) = if(base.getNullable(index)) base.getInt(index) else null
			fun getNullableBoolean(index: Int) = if(base.getNullable(index)) base.getBoolean(index) else null
			fun getNullableDouble(index: Int) = if(base.getNullable(index)) base.getDouble(index) else null
			fun getNullableLong(index: Int) = if(base.getNullable(index)) base.getLong(index) else null
			fun getNullableMap(index: Int) = if(base.getNullable(index)) base.getMap(index) else null
			fun getNullableString(index: Int) = if(base.getNullable(index)) base.getString(index) else null
			fun getNullableBytes(index: Int) = if(base.getNullable(index)) base.getBytes(index) else null
			fun getNullableEnum(index: Int, klass: Class<out Enum<*>>) =
				if(base.getNullable(index)) base.getEnum(index, klass) else null
			
			
			fun getDefault(index: Int) = if(base.getDefault(index)) base.getAny(index) else null
			fun getDefaultInt(index: Int) = if(base.getDefault(index)) base.getInt(index) else null
			fun getDefaultBoolean(index: Int) = if(base.getDefault(index)) base.getBoolean(index) else null
			fun getDefaultDouble(index: Int) = if(base.getDefault(index)) base.getDouble(index) else null
			fun getDefaultLong(index: Int) = if(base.getDefault(index)) base.getLong(index) else null
			fun getDefaultMap(index: Int) = if(base.getDefault(index)) base.getMap(index) else null
			fun getDefaultString(index: Int) = if(base.getDefault(index)) base.getString(index) else null
			fun getDefaultBytes(index: Int) = if(base.getDefault(index)) base.getBytes(index) else null
			fun getDefaultEnum(index: Int, klass: Class<out Enum<*>>) =
				if(base.getDefault(index)) base.getEnum(index, klass) else null
			
			
			fun getDefaultNullable(index: Int) = if(base.getDefaultNullable(index)) base.getAny(index) else null
			fun getDefaultNullableInt(index: Int) = if(base.getDefaultNullable(index)) base.getInt(index) else null
			fun getDefaultNullableBoolean(index: Int) =
				if(base.getDefaultNullable(index)) base.getBoolean(index) else null
			
			fun getDefaultNullableDouble(index: Int) =
				if(base.getDefaultNullable(index)) base.getDouble(index) else null
			
			fun getDefaultNullableLong(index: Int) = if(base.getDefaultNullable(index)) base.getLong(index) else null
			fun getDefaultNullableMap(index: Int) = if(base.getDefaultNullable(index)) base.getMap(index) else null
			fun getDefaultNullableString(index: Int) =
				if(base.getDefaultNullable(index)) base.getString(index) else null
			
			fun getDefaultNullableBytes(index: Int) = if(base.getDefaultNullable(index)) base.getBytes(index) else null
			fun getDefaultNullableEnum(index: Int, klass: Class<out Enum<*>>) =
				if(base.getDefaultNullable(index)) base.getEnum(index, klass) else null
		}
		
		private val staticLookup = MethodHandles.lookup()
		
		// Arguments
		private val extraTypes = listOf(
			ArgumentsHelper::class.java, // arguments
			RawLuaTable::class.java, // self
		)
		
		private val argumentGetter = MethodHandles.filterReturnValue(
			extraTypes.pickArgumentHandle(0),
			handle(ArgumentsHelper::base.getter),
		)
		private val selfGetter = extraTypes.pickArgumentHandle(1)
		
		// Helpers
		private val optionalAsNullable = handle(Helper::asNullable)
		private val getBackingObject = handle(Helper::getBackingObject)
		private val defaultBitFlag = handle(Helper::defaultBitFlag)
		
		private fun handle(function: KFunction<*>) =
			staticLookup.unreflect(function.javaMethod!!)
		
		
		private val argTypes = listOf(
			handle(ArgumentsHelper::get),
			handle(ArgumentsHelper::getInt),
			handle(ArgumentsHelper::getBoolean),
			handle(ArgumentsHelper::getDouble),
			handle(ArgumentsHelper::getLong),
			handle(ArgumentsHelper::getMap),
			handle(ArgumentsHelper::getString),
			handle(ArgumentsHelper::getBytes),
		).associateBy { it.type().returnType() }
		
		// private val _argNullableTypes = argTypes.mapValues { (_, handle) -> MethodHandles.tableSwitch() }
		
		private val argNullableTypes = listOf(
			handle(ArgumentsHelper::getNullable),
			handle(ArgumentsHelper::getNullableInt),
			handle(ArgumentsHelper::getNullableBoolean),
			handle(ArgumentsHelper::getNullableDouble),
			handle(ArgumentsHelper::getNullableLong),
			handle(ArgumentsHelper::getNullableMap),
			handle(ArgumentsHelper::getNullableString),
			handle(ArgumentsHelper::getNullableBytes),
		).associateBy { it.type().returnType() }
		
		private val argDefaultTypes = listOf(
			handle(ArgumentsHelper::getDefault),
			handle(ArgumentsHelper::getDefaultInt),
			handle(ArgumentsHelper::getDefaultBoolean),
			handle(ArgumentsHelper::getDefaultDouble),
			handle(ArgumentsHelper::getDefaultLong),
			handle(ArgumentsHelper::getDefaultMap),
			handle(ArgumentsHelper::getDefaultString),
			handle(ArgumentsHelper::getDefaultBytes),
		).associateBy { it.type().returnType() }
		
		private val argDefaultNullableTypes = listOf(
			handle(ArgumentsHelper::getDefaultNullable),
			handle(ArgumentsHelper::getDefaultNullableInt),
			handle(ArgumentsHelper::getDefaultNullableBoolean),
			handle(ArgumentsHelper::getDefaultNullableDouble),
			handle(ArgumentsHelper::getDefaultNullableLong),
			handle(ArgumentsHelper::getDefaultNullableMap),
			handle(ArgumentsHelper::getDefaultNullableString),
			handle(ArgumentsHelper::getDefaultNullableBytes),
		).associateBy { it.type().returnType() }
		
		// private val argDeserializeType = handle(LuaArguments::deserialize)
		
		private val argEnumType = handle(ArgumentsHelper::getEnum)
		private val argNullableEnumType = handle(ArgumentsHelper::getNullableEnum)
		private val argDefaultEnumType = handle(ArgumentsHelper::getDefaultEnum)
		private val argDefaultNullableEnumType = handle(ArgumentsHelper::getDefaultNullableEnum)
		
		
		private val resultUnit = staticLookup.findStatic(
			MethodResult::class.java,
			"of",
			MethodType.methodType(MethodResult::class.java)
		)
		
		private val resultAny = staticLookup.findStatic(
			MethodResult::class.java,
			"of",
			MethodType.methodType(MethodResult::class.java, Any::class.java)
		)
		
		private val resultArrayVararg = staticLookup.findStatic(
			MethodResult::class.java,
			"of",
			MethodType.methodType(MethodResult::class.java, Array<Any?>::class.java)
		)
		private val resultVararg = MethodHandles.filterReturnValue(
			staticLookup.findVirtual(
				LuaVararg::class.java,
				"getValues",
				MethodType.methodType(Array<Any?>::class.java)
			),
			resultArrayVararg,
		)
	}
	
	override val name: String = annotation.value.ifBlank { function.name }
	
	private val nonOptionalCount: Int
	private val handle: MethodHandle
	
	init {
		val parameters = function.parameters // including instance receiver
		nonOptionalCount = parameters.indexOfLast { !it.isOptional } + 1
		
		// handle arguments
		var handle: MethodHandle
		
		if(nonOptionalCount == parameters.size) {
			handle = impl.lookup.unreflect(function.javaMethod!!)
			handle = MethodHandles.dropArguments(handle, parameters.size, extraTypes)
			// handle: (this: ?, self: RawLuaTable, args: IArguments, *Params) -> Result
			
			for(parameter in parameters.asReversed()) {
				val index = parameter.index
				
				// leaves (this: ?) instance receiver in final MethodHandle
				if(index == 0) continue
				
				val type = parameter.type
				val typeClass = type.jvmErasure.java
				val isNullable = type.isMarkedNullable
				
				// combiner: (self: RawLuaTable, args: IArguments) -> EachArgumentType
				val combiner = when {
					typeClass == IArguments::class.java ->
						argumentGetter
					
					parameter.hasAnnotation<LuaSelf>() -> {
						if(typeClass != RawLuaTable::class.java) {
							throw IllegalStateException("only supports RawLuaTable as @LuaSelf value parameter")
						}
						selfGetter
					}
					
					typeClass.isEnum ->
						MethodHandles.insertArguments(
							if(isNullable) argNullableEnumType else argEnumType,
							1,
							index, typeClass
						)
					
					// typeClass.isAnnotationPresent(LuaObject::class.java) ->
					// 	MethodHandles.insertArguments(argDeserializeType, 1, index)
					
					else -> {
						val target = if(isNullable) argNullableTypes[typeClass] else argTypes[typeClass]
							?: error("unsupported type $typeClass of parameter ${function.name}/${parameter.name}")
						
						MethodHandles.insertArguments(target, 1, index)
					}
				}
				
				handle = MethodHandles.foldArguments(handle, index, combiner)
			}
		} else {
			if(true) TODO("not done yet")
			// hasOptional == true
			handle = impl.lookup.unreflect(function.javaDefaultMethod!!)
			handle = MethodHandles.dropArguments(handle, parameters.size, extraTypes)
			// handle: Target.(self: RawLuaTable, args: IArguments, *Params, flags: Int, marker: Any) -> Result
			
			handle = MethodHandles.insertArguments(handle, handle.type().parameterCount() - 1, null)
			handle = MethodHandles.foldArguments(handle, handle.type().parameterCount() - 1, defaultBitFlag)
			
			for((index, parameter) in parameters.withIndex().reversed()) {
				val type = parameter.type.jvmErasure.java
				val isNullable = parameter.type.isMarkedNullable
				val isOptional = index > nonOptionalCount
				
				// combiner: (self: RawLuaTable, args: IArguments) -> EachArgumentType
				val combiner = when {
					type == IArguments::class.java -> argumentGetter
					
					parameter.hasAnnotation<LuaSelf>() -> {
						if(type != RawLuaTable::class.java) {
							throw IllegalStateException("only supports RawLuaTable as @LuaSelf value parameter")
						}
						selfGetter
					}
					
					Enum::class.java.isAssignableFrom(type) && type != Enum::class.java ->
						MethodHandles.insertArguments(
							if(isOptional) {
								if(isNullable) argDefaultNullableEnumType else argDefaultEnumType
							} else {
								if(isNullable) argNullableEnumType else argEnumType
							},
							1,
							index, type
						)
					
					parameter.type.hasAnnotation<LuaObject>() ->
						TODO("1. try to recover; 2. try to deserialize(data class; @LuaObject.Deserialize) 3. optional")
					
					else -> {
						val target = if(isOptional) {
							if(isNullable) argDefaultNullableTypes[type] else argDefaultTypes[type]
						} else {
							if(isNullable) argNullableTypes[type] else argTypes[type]
						}
							?: error("unsupported type $type of parameter ${function.name}/${parameter.name}")
						
						MethodHandles.insertArguments(target, 1, index)
					}
				}
				handle = MethodHandles.foldArguments(handle, index, combiner)
			}
		}
		// handle: Target.(self: RawLuaTable, args: IArguments) -> Result
		
		// handle return value
		// val returnType = function.returnType.jvmErasure.java
		// when {
		// 	returnType == MethodResult::class.java -> {}
		//
		// 	returnType == Void.TYPE ->
		// 		handle = MethodHandles.filterReturnValue(handle, resultUnit)
		//
		// 	returnType == LuaVararg::class.java ->
		// 		handle = MethodHandles.filterReturnValue(handle, resultVararg)
		//
		// 	else -> handle = MethodHandles.filterReturnValue(
		// 		handle.asType(handle.type().changeReturnType(Any::class.java)),
		// 		resultAny
		// 	)
		// }
		
		this.handle = handle
	}
	
	
	override suspend fun invoke(self: Any, arguments: LuaArguments): Any? {
		if(arguments.size < nonOptionalCount) {
			throw LuaError("not enough arguments provided; expected ${arguments.size} or more arguments")
		}
		
		val luaSelf = LuaMemberContext.current.luaSelf
		return handle.invoke(self, luaSelf, ArgumentsHelper(arguments))
	}
	
	override fun toString(): String =
		"MethodMetadata(name=$name, handle=$handle)"
}

private class KClassCaches {
	private val cache = mutableMapOf<KClass<*>, KClassCache?>()
	
	fun get(clazz: KClass<*>) = cache.getOrPut(clazz) {
		val parent = clazz.superclasses.firstOrNull { !it.java.isInterface }
		parent?.let { KClassCache(clazz, it) }
	}
}

private class KClassCache(clazz: KClass<*>, val parent: KClass<*>) {
	val overrides: Map<KCallable<*>, KCallable<*>>
	
	init {
		val parentSignatures = parent.members.groupBy { it.name }
		fun findParent(member: KCallable<*>): Pair<KCallable<*>, KCallable<*>>? {
			val candidates = (parentSignatures[member.name] ?: return null)
				.filter { it.parameters.size == member.parameters.size }
			
			outer@ for(candidate in candidates) {
				for((index, parentParameter) in candidate.parameters.withIndex()) {
					val childParameter = member.parameters[index]
					if(!childParameter.type.isSupertypeOf(parentParameter.type)) {
						continue@outer
					}
				}
				return member to candidate
			}
			
			return null
		}
		overrides = clazz.members.mapNotNull { findParent(it) }.toMap()
	}
}

private fun <T : Annotation> KCallable<*>.findAnnotationInherited(
	clazz: KClass<*>,
	target: KClass<T>,
	caches: KClassCaches,
): T? {
	annotations.firstOrNull { it.annotationClass == target }?.let {
		@Suppress("UNCHECKED_CAST")
		return it as T
	}
	
	val cache = caches.get(clazz) ?: return null
	val parent = cache.overrides[this] ?: return null
	return parent.findAnnotationInherited(cache.parent, target, caches)
}
