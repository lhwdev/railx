package com.lhwdev.minecraft.railx.cc

import com.lhwdev.minecraft.railx.utils.map
import com.lhwdev.minecraft.railx.utils.mapIndexed
import org.squiddev.cobalt.*
import java.util.*
import kotlin.collections.AbstractList
import kotlin.collections.AbstractMap
import kotlin.collections.AbstractSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


private val DynamicType = 100

val LuaValue.dynamicTable: DynamicLuaTable?
	get() = this as? DynamicLuaTable


fun List<LuaValue>.asDynamicLuaList(): LuaValue = object : DynamicLuaList() {
	override val base = this@asDynamicLuaList
}

fun MutableList<LuaValue>.asMutableDynamicLuaList(): LuaValue = object : MutableDynamicLuaList() {
	override val base = this@asMutableDynamicLuaList
}

fun Map<LuaValue, LuaValue>.asDynamicLuaMap(): LuaValue = object : DynamicLuaMap() {
	override val base = this@asDynamicLuaMap
}

fun MutableMap<LuaValue, LuaValue>.asMutableDynamicLuaMap(): LuaValue = object : MutableDynamicLuaMap() {
	override val base = this@asMutableDynamicLuaMap
}


private fun readOnlyError(): Nothing = throw LuaError("this table is read only")


abstract class DynamicLuaList : LuaValue(DynamicType), DynamicLuaTable {
	protected abstract val base: List<LuaValue>
	
	override val size: Int
		get() = base.size
	
	override fun rawGet(key: LuaValue): LuaValue =
		if(key is LuaInteger) rawGet(key.v) else Constants.NIL
	
	override fun rawGet(key: Int): LuaValue =
		base[key]
	
	override fun pairs(table: LuaValue): Iterator<Pair<LuaValue, LuaValue>> =
		base.iterator().mapIndexed { index, value -> ValueFactory.valueOf(index) to value }
	
	override fun indexedPairs(table: LuaValue): Iterator<LuaValue> =
		base.iterator()
	
	override fun next(table: LuaValue, index: LuaValue): Varargs = if(index is LuaInteger) {
		ValueFactory.varargsOf(
			ValueFactory.valueOf(index.v + 1),
			next(table, index.v)
		)
	} else {
		Constants.NIL
	}
	
	override fun next(table: LuaValue, index: Int): LuaValue? = if(index < base.size - 1) {
		val targetIndex = index + 1
		base[targetIndex]
	} else {
		null
	}
	
	override fun rawSet(key: LuaValue, value: LuaValue): Boolean =
		readOnlyError()
	
	override fun rawSet(key: Int, value: LuaValue): Boolean =
		readOnlyError()
	
	override fun remove(index: Int): LuaValue =
		readOnlyError()
	
	override fun insert(index: Int, value: LuaValue): Unit =
		readOnlyError()
	
	override fun duplicate(from: Int, to: Int, count: Int): Unit =
		readOnlyError()
}

abstract class MutableDynamicLuaList : DynamicLuaList() {
	abstract override val base: MutableList<LuaValue>
	
	override fun rawSet(key: LuaValue, value: LuaValue): Boolean =
		if(key is LuaInteger) rawSet(key.v, value) else false
	
	override fun rawSet(key: Int, value: LuaValue): Boolean {
		base[key] = value
		return false
	}
	
	override fun remove(index: Int): LuaValue =
		base.removeAt(index)
	
	override fun insert(index: Int, value: LuaValue) {
		base.add(index, value)
	}
	
	// override fun duplicate(from: Int, to: Int, count: Int)
}


abstract class DynamicLuaMap : LuaValue(DynamicType), DynamicLuaTable {
	protected abstract val base: Map<LuaValue, LuaValue>
	
	override val size: Int
		get() = base.size
	
	override fun rawGet(key: LuaValue): LuaValue =
		base[key] ?: Constants.NIL
	
	override fun pairs(table: LuaValue): Iterator<Pair<LuaValue, LuaValue>> =
		base.iterator().map { (key, value) -> key to value }
	
	override fun next(table: LuaValue, index: LuaValue): Varargs {
		val iterator = base.iterator()
		for((key, _) in iterator) {
			if(key == index) {
				return if(iterator.hasNext()) {
					val (nextKey, nextValue) = iterator.next()
					ValueFactory.varargsOf(nextKey, nextValue)
				} else {
					Constants.NONE
				}
			}
		}
		return Constants.NONE
	}
	
	override fun rawSet(key: LuaValue, value: LuaValue): Boolean =
		readOnlyError()
}

abstract class MutableDynamicLuaMap : DynamicLuaMap() {
	abstract override val base: MutableMap<LuaValue, LuaValue>
	
	override fun rawSet(key: LuaValue, value: LuaValue): Boolean {
		base[key] = value
		return true
	}
	
	override fun trySet(key: LuaValue, value: LuaValue): Boolean {
		return if(key in base) {
			base[key] = value
			true
		} else {
			false
		}
	}
}


/// Dynamic evaluation (List<Any?>, Map<Any?, Any?> -> LuaValue)

abstract class ConvertedLuaList(private val machine: LuaMachineAccess) : AbstractList<LuaValue>() {
	protected abstract val original: List<*>
	
	// TODO: huge rooms for improvement
	private val mappingCache = IdentityHashMap<Any?, LuaValue>()
	
	protected fun getFor(original: Any?) =
		mappingCache.getOrPut(original) { machine.convertToValue(original, mappingCache) }
	
	
	override val size: Int
		get() = original.size
	
	override fun get(index: Int): LuaValue = getFor(original[index])
	
	override fun iterator(): Iterator<LuaValue> =
		original.iterator().map { getFor(it) }
	
	override fun listIterator(): ListIterator<LuaValue> = listIterator(0)
	
	override fun listIterator(index: Int): ListIterator<LuaValue> =
		ListIteratorImpl(index)
	
	private inner class ListIteratorImpl(index: Int) : ListIterator<LuaValue> {
		private val backing = original.listIterator(index)
		override fun hasNext(): Boolean = backing.hasNext()
		override fun hasPrevious(): Boolean = backing.hasPrevious()
		override fun next(): LuaValue = getFor(backing.next())
		override fun nextIndex(): Int = backing.nextIndex()
		override fun previous(): LuaValue = getFor(backing.previous())
		override fun previousIndex(): Int = backing.previousIndex()
	}
}

abstract class MutableConvertedLuaList(private val machine: LuaMachineAccess) : AbstractMutableList<LuaValue>() {
	protected abstract val original: MutableList<Any?>
	
	// TODO: huge rooms for improvement
	private val mappingCache = IdentityHashMap<Any?, LuaValue>()
	
	protected fun getFor(original: Any?) =
		mappingCache.getOrPut(original) { machine.convertToValue(original, mappingCache) }
	
	
	override val size: Int
		get() = original.size
	
	override fun get(index: Int): LuaValue = getFor(original[index])
	
	override fun iterator(): MutableIterator<LuaValue> = object : MutableIterator<LuaValue> {
		private val backing = original.iterator()
		override fun hasNext(): Boolean = backing.hasNext()
		override fun next(): LuaValue = getFor(backing.next())
		override fun remove() = backing.remove()
	}
	
	override fun listIterator(): MutableListIterator<LuaValue> = listIterator(0)
	
	override fun listIterator(index: Int): MutableListIterator<LuaValue> =
		ListIteratorImpl(index)
	
	private inner class ListIteratorImpl(index: Int) : MutableListIterator<LuaValue> {
		private val backing = original.listIterator(index)
		override fun add(element: LuaValue) = backing.add(machine.convertFromValue(element))
		override fun remove() = backing.remove()
		override fun set(element: LuaValue) = backing.set(machine.convertFromValue(element))
		override fun hasNext(): Boolean = backing.hasNext()
		override fun hasPrevious(): Boolean = backing.hasPrevious()
		override fun next(): LuaValue = getFor(backing.next())
		override fun nextIndex(): Int = backing.nextIndex()
		override fun previous(): LuaValue = getFor(backing.previous())
		override fun previousIndex(): Int = backing.previousIndex()
	}
}

abstract class ConvertedLuaMap(private val machine: LuaMachineAccess) : AbstractMap<LuaValue, LuaValue>() {
	protected abstract val original: Map<*, *>
	
	// TODO: huge rooms for improvement
	private val mappingCache = IdentityHashMap<Any?, LuaValue>()
	
	protected fun getFor(original: Any?) =
		mappingCache.getOrPut(original) { machine.convertToValue(original, mappingCache) }
	
	private var previousKeys = emptySet<Any?>()
	private var mapped = emptyMap<LuaValue, Any?>()
		get() {
			if(previousKeys != original.keys) {
				previousKeys = original.keys.toSet()
				field = original.mapKeys { (key, _) -> getFor(key) }
			}
			return field
		}
	
	
	override val entries: Set<Map.Entry<LuaValue, LuaValue>> = object : AbstractSet<Entry>() {
		override val size: Int
			get() = original.size
		
		override fun iterator(): Iterator<Entry> =
			mapped.iterator().map { (key, value) -> Entry(key, getFor(value)) }
	}
	
	override val keys: Set<LuaValue>
		get() = mapped.keys
	
	override fun containsKey(key: LuaValue): Boolean =
		mapped.containsKey(key)
	
	override fun get(key: LuaValue): LuaValue? =
		mapped[key]?.let { getFor(it) }
	
	private class Entry(override val key: LuaValue, override val value: LuaValue) : Map.Entry<LuaValue, LuaValue>
}

abstract class MutableConvertedLuaMap(private val machine: LuaMachineAccess) :
	AbstractMutableMap<LuaValue, LuaValue>() {
	protected abstract val original: MutableMap<Any?, Any?>
	
	// TODO: huge rooms for improvement
	private val mappingCache = IdentityHashMap<Any?, LuaValue>()
	
	protected fun getFor(original: Any?) =
		mappingCache.getOrPut(original) { machine.convertToValue(original, mappingCache) }
	
	protected fun fromValue(value: LuaValue) =
		mappingCache.entries.find { it.value == value }?.key ?: machine.convertFromValue(value)
	
	private var previousKeys = emptySet<Any?>()
	private var mapped = emptyMap<LuaValue, MutableMap.MutableEntry<Any?, Any?>>() as MutableMap
		get() {
			if(previousKeys != original.keys) {
				previousKeys = original.keys.toSet()
				field = HashMap<LuaValue, MutableMap.MutableEntry<Any?, Any?>>().apply {
					for(entry in original.entries) {
						set(getFor(entry.key), entry)
					}
				}
			}
			return field
		}
	
	
	override val entries = object : AbstractMutableSet<MutableMap.MutableEntry<LuaValue, LuaValue>>() {
		override fun add(element: MutableMap.MutableEntry<LuaValue, LuaValue>): Boolean = error("entries")
		
		override val size: Int
			get() = original.size
		
		override fun iterator() = object : MutableIterator<MutableMap.MutableEntry<LuaValue, LuaValue>> {
			private val backing = mapped.iterator()
			private var last: Entry? = null
			
			override fun hasNext(): Boolean = backing.hasNext()
			override fun next(): MutableMap.MutableEntry<LuaValue, LuaValue> {
				val (key, entry) = backing.next()
				return Entry(entry, key, getFor(entry.value)).also { last = it }
			}
			
			override fun remove() {
				original.remove(last!!.backing.key)
			}
		}
	}
	
	override val keys: MutableSet<LuaValue> = object : AbstractMutableSet<LuaValue>() {
		override fun add(element: LuaValue): Boolean = error("keys")
		override fun remove(element: LuaValue): Boolean {
			return super.remove(element)
		}
		
		override fun iterator(): MutableIterator<LuaValue> = entries.iterator().map { it.key }
		
		override val size: Int
			get() = original.size
	}
	
	override fun containsKey(key: LuaValue): Boolean =
		mapped.containsKey(key)
	
	override fun get(key: LuaValue): LuaValue? =
		mapped[key]?.let { getFor(it) }
	
	override fun put(key: LuaValue, value: LuaValue): LuaValue? {
		val previous = original.put(fromValue(key), fromValue(value))
		return getFor(previous)
	}
	
	private inner class Entry(
		val backing: MutableMap.MutableEntry<Any?, Any?>,
		override val key: LuaValue,
		override val value: LuaValue,
	) :
		MutableMap.MutableEntry<LuaValue, LuaValue> {
		override fun setValue(newValue: LuaValue): LuaValue {
			val previous = backing.setValue(fromValue(newValue))
			mappingCache[backing.value] = newValue
			return getFor(previous) // this must be cached, isn't it?
		}
	}
}
