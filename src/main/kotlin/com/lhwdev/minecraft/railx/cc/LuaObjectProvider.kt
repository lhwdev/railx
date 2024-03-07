package com.lhwdev.minecraft.railx.cc

import com.lhwdev.minecraft.railx.api.lua.LuaArguments
import com.lhwdev.minecraft.railx.api.lua.LuaObjectProvidable
import com.lhwdev.minecraft.railx.api.lua.LuaVararg
import com.lhwdev.minecraft.railx.api.lua.RawLuaTable
import com.lhwdev.minecraft.railx.lua.LuaExecutionContext
import com.lhwdev.minecraft.railx.lua.LuaMemberContext
import com.lhwdev.minecraft.railx.lua.asm.*
import kotlinx.coroutines.withContext
import org.squiddev.cobalt.*
import org.squiddev.cobalt.function.LuaFunction
import java.util.*


val LuaObjectBackingKey = LuaString.valueOf("luaObject_backing")

fun LuaTable.getBackingObject(): Any? = getMetatable(null).rawget(LuaObjectBackingKey)
	.optUserdata(Any::class.java, null)


class LuaObjectProvider(val context: LuaExecutionContext, val machine: LuaMachineAccess) {
	val cache = IdentityHashMap<Any?, LuaValue>()
	
	val tableSupplier: LuaTableSupplier = LuaTableSupplierImpl()
	
	val bridge = LuaBridgeFunctionContextImpl(context)
	
	
	fun createLuaObject(value: Any): LuaTable {
		println("createLuaObject $value")
		var target = value
		while(target is LuaObjectProvidable) {
			val result = target.provideObject()
			if(result == target) break
			target = result
		}
		if(target is ObjectMetadata) {
			return LuaObjectBuilder(this).apply { addToTable(target) }.build()
		}
		return LuaObjectBuilder(this).apply { addObject(target) }.build()
	}
}


private fun LuaMachineAccess.mapReturnValue(value: Any?): Varargs = if(value is LuaVararg) {
	convertToValues(value.values)
} else {
	convertToValue(value)
}


class LuaObjectBuilder(val provider: LuaObjectProvider) {
	private val machine get() = provider.machine
	
	private val table = LuaTable()
	val metatable = LuaTable().also { table.setMetatable(null, it) }
	
	private val dynamicGetterResolvers = mutableListOf<(name: String) -> Varargs?>().also { resolvers ->
		metatable.rawset(Constants.INDEX, provider.bridge.createFunction { args ->
			val name = args.arg(2).checkString()
			
			for(resolver in resolvers) {
				val result = resolver(name)
				if(result != null) return@createFunction result
			}
			Constants.NIL
		})
	}
	private val dynamicSetterResolvers =
		mutableListOf<(name: String, value: LuaValue) -> Varargs?>().also { resolvers ->
			metatable.rawset(Constants.NEWINDEX, provider.bridge.createFunction { args ->
				val name = args.arg(2).checkString()
				val value = args.arg(3)
				
				for(resolver in resolvers) {
					val result = resolver(name, value)
					if(result != null) return@createFunction result
				}
				Constants.NIL
			})
		}
	
	
	fun addObject(value: Any) {
		val metadata = provider.tableSupplier.getObjectMetadata(value)
		addToTable(metadata)
	}
	
	// TODO: barely optimized codes; can I call this code or something terrible shit patches
	fun addToTable(obj: ObjectMetadata) {
		println("addToTable $obj")
		for(field in obj.knownFields) {
			addField(field)
		}
		for(method in obj.staticMethods) {
			addMethod(method, obj.value)
		}
		
		// dynamic resolution from here
		addProperties(obj.staticProperties, obj.value)
		
		addDynamicItem(obj)
		
		for(parent in obj.parents) {
			addParent(parent, obj.value)
		}
		
		metatable.rawset(LuaObjectBackingKey, LuaUserdata(obj.value))
	}
	
	fun getField(field: FieldMetadata): LuaValue {
		return machine.convertToValue(field.value, provider.cache)
	}
	
	fun addField(field: FieldMetadata) {
		table.rawset(field.name, getField(field))
	}
	
	fun getMethod(method: MethodMetadata, self: Any): LuaFunction {
		return provider.bridge.createFunction { args ->
			withMyContext { machine.mapReturnValue(method.invoke(self, VarargArguments.of(args) as LuaArguments)) }
		}
	}
	
	fun addMethod(method: MethodMetadata, self: Any) {
		table.rawset(method.name, getMethod(method, self))
	}
	
	fun getProperty(property: PropertyMetadata, self: Any): Varargs {
		return provider.bridge.createFunction {
			withMyContext { machine.mapReturnValue(property.get(self)) }
		}.invoke(machine.state, ValueFactory.varargsOf())
	}
	
	fun setProperty(property: PropertyMetadata, self: Any, value: LuaValue): Varargs {
		return provider.bridge.createFunction {
			withMyContext { machine.mapReturnValue(property.set(self, value)) }
		}.invoke(machine.state, ValueFactory.varargsOf())
	}
	
	fun addProperties(properties: List<PropertyMetadata>, self: Any) {
		val propertiesMap = properties.associateBy { it.name }
		
		for(property in properties) {
			table.rawset(property.name, Constants.TRUE) // setting dummy; to show up in keys()
		}
		
		dynamicGetterResolvers += { name ->
			propertiesMap[name]?.let { property ->
				getProperty(property, self)
			}
		}
		dynamicSetterResolvers += { name, value ->
			propertiesMap[name]?.let { property ->
				setProperty(property, self, value)
			}
		}
	}
	
	fun getDynamicItem(item: ItemMetadata, self: Any): Varargs = when(item) {
		is FieldMetadata -> getField(item)
		is MethodMetadata -> getMethod(item, self)
		is PropertyMetadata -> getProperty(item, self)
	}
	
	fun setDynamicItem(item: ItemMetadata, self: Any, value: LuaValue): Varargs? = when(item) {
		is FieldMetadata -> null
		is MethodMetadata -> null
		is PropertyMetadata -> setProperty(item, self, value)
	}
	
	private inline fun addDynamicItemBase(self: Any, crossinline findItem: (name: String) -> ItemMetadata?) {
		dynamicGetterResolvers += { name ->
			findItem(name)?.let { item ->
				getDynamicItem(item, self)
			}
		}
		dynamicSetterResolvers += { name, value ->
			findItem(name)?.let { item ->
				setDynamicItem(item, self, value)
			}
		}
	}
	
	fun addDynamicItem(obj: ObjectMetadata) {
		addDynamicItemBase(obj.value) { obj.findDynamicItem(it) }
	}
	
	fun addParent(parent: ObjectMetadataLayer, self: Any) {
		val map = parent.knownToMap()
		addDynamicItemBase(self) { map[it] }
	}
	
	fun build(): LuaTable = table
	
	
	private suspend fun <R> withMyContext(block: suspend () -> R): R {
		val memberContext = object : LuaMemberContext {
			override val luaSelf: RawLuaTable = // TODO
				object : RawLuaTable, Map<Any?, Any?> by mapOf() {
					override fun get(key: Any?) = TODO()
				}
		}
		
		return withContext(
			(LuaMemberContext provides memberContext)
		) {
			block()
		}
	}
}
