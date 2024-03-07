package com.lhwdev.minecraft.railx.utils

private class IteratorIterable<T>(private val iterator: Iterator<T>) : Iterable<T> {
	private var used = false
	
	override fun iterator(): Iterator<T> {
		if(used) error("used iterator")
		used = true
		return iterator
	}
}

fun <T> Iterator<T>.asIterable(): Iterable<T> = IteratorIterable(this)

inline fun <T, R> Iterator<T>.map(crossinline block: (T) -> R): Iterator<R> = object : Iterator<R> {
	override fun hasNext() = this@map.hasNext()
	override fun next() = block(this@map.next())
}

@JvmName("mapMutable")
inline fun <T, R> MutableIterator<T>.map(crossinline block: (T) -> R): MutableIterator<R> =
	object : MutableIterator<R> {
		override fun hasNext() = this@map.hasNext()
		override fun next() = block(this@map.next())
		override fun remove() {
			this@map.remove()
		}
	}

inline fun <T, R> Iterator<T>.mapIndexed(crossinline block: (index: Int, T) -> R): Iterator<R> = object : Iterator<R> {
	private var index = 0
	override fun hasNext() = this@mapIndexed.hasNext()
	override fun next() = block(index++, this@mapIndexed.next())
}

private val Empty = Any()

@PublishedApi
internal abstract class MapNotNullIterator<T, R>(private val backing: Iterator<T>) : Iterator<R> {
	private var buffer: Any? = Empty
	
	protected abstract fun map(value: T): R?
	
	private fun read() {
		while(backing.hasNext()) {
			buffer = map(backing.next()) ?: continue
			return
		}
		buffer = Empty
	}
	
	init {
		read()
	}
	
	
	final override fun hasNext(): Boolean =
		buffer != Empty
	
	final override fun next(): R =
		@Suppress("UNCHECKED_CAST") (buffer as R)
	
	init {
		read()
	}
}

private abstract class FilterIterator<T>(backing: Iterator<T>) : MapNotNullIterator<T, T>(backing) {
	protected abstract fun predicate(value: T): Boolean
	
	final override fun map(value: T): T? = value.takeIf { predicate(it) }
}

inline fun <T, R> Iterator<T>.mapNotNull(crossinline block: (T) -> R?): Iterator<R> =
	object : MapNotNullIterator<T, R>(backing = this@mapNotNull) {
		override fun map(value: T): R? = block(value)
	}

inline fun <T> Iterator<T>.filter(crossinline block: (T) -> Boolean): Iterator<T> =
	mapNotNull { it.takeIf(block) }
