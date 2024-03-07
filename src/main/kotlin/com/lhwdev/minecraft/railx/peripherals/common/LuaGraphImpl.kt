package com.lhwdev.minecraft.railx.peripherals.common

import com.lhwdev.minecraft.railx.utils.asIterable
import com.lhwdev.minecraft.railx.utils.map


private abstract class LuaGraphIteratorBase<N : Any, E : Any>(private val impl: LuaGraphImpl<N, E>, head: N) :
	LuaGraphIterator<N, E> {
	override var current = head
	
	protected abstract fun read(): N
	
	override fun nextNode(): N =
		read().also { current = it }
	
	override fun next(): LuaGraphIterator.Next<N, E> {
		val previous = current
		val node = nextNode()
		return LuaGraphIterator.Next(node, impl.getEdge(previous, node)!!)
	}
	
	override fun iterateFromHere(method: LuaGraphIterator.Method?) =
		impl.iterator(method, current)
	
	override fun toGraph() = impl.duplicateGraph(head = current)
	
	init {
		@Suppress("LeakingThis")
		read()
	}
}


abstract class LuaGraphImpl<N : Any, E : Any>(override val head: N?) : LuaGraph<N, E> {
	interface Delegate<N, E> {
		val defaultIterateMethod: LuaGraphIterator.Method
			get() = LuaGraphIterator.Method.Bfs
		
		fun childrenOfNode(node: N): Collection<N>
		
		fun getEdge(from: N, to: N): E?
		
		fun getEdgeFrom(edge: E): N
		
		fun getEdgeTo(edge: E): N
	}
	
	
	protected abstract val delegate: Delegate<N, E>
	
	
	open fun duplicateGraph(head: N): LuaGraphImpl<N, E> = object : LuaGraphImpl<N, E>(head) {
		override val delegate: Delegate<N, E> = this@LuaGraphImpl.delegate
	}
	
	
	// do not call this
	override val size: Int
		get() = count { true } // Note: (this as Iterable<N>).count() uses (this as? Collection)?.size
	
	override fun contains(element: N): Boolean =
		nodes.contains(element)
	
	override fun containsAll(elements: Collection<N>): Boolean =
		elements.all { nodes.contains(it) }
	
	override fun isEmpty(): Boolean =
		head == null
	
	
	private fun nodes(): Iterator<N> {
		var gotHead = false
		val iterator = iterator(method = null)
			?: return emptyList<N>().iterator()
		
		return object : Iterator<N> {
			override fun hasNext() = !gotHead || iterator.hasNext()
			override fun next() = if(gotHead) {
				iterator.nextNode()
			} else {
				gotHead = true
				iterator.current
			}
		}
	}
	
	private fun nodesWithoutFirst(): Iterator<N>? =
		iterator(method = null)?.map { it.node }
	
	override fun iterator() = nodes()
	
	override fun iterator(method: LuaGraphIterator.Method?): LuaGraphIterator<N, E>? {
		val head = head ?: return null
		return iterator(method, head)
	}
	
	fun iterator(method: LuaGraphIterator.Method?, head: N): LuaGraphIterator<N, E> =
		when(method ?: delegate.defaultIterateMethod) {
			LuaGraphIterator.Method.Bfs -> bfsIterator(head)
			LuaGraphIterator.Method.Dfs -> TODO()
		}
	
	private fun bfsIterator(head: N) = object : LuaGraphIteratorBase<N, E>(this, head) {
		val items = ArrayDeque<N>().also { it += head }
		val visited = LinkedHashSet<N>().also { it += head }
		
		override fun read(): N {
			val nextHead = items.removeFirst()
			for(child in delegate.childrenOfNode(nextHead)) {
				if(child !in visited) {
					items += child
					visited += child
				}
			}
			return nextHead
		}
		
		override fun hasNext() = items.isNotEmpty()
	}
	
	override fun hasNode(node: N): Boolean = node in nodes().asIterable()
	
	override fun getEdge(from: N, to: N): E? =
		delegate.getEdge(from, to)
	
	override fun hasEdge(from: N, to: N): Boolean =
		delegate.getEdge(from, to) != null
	
	override val nodes: Collection<N>
		get() = object : Collection<N> {
			override val size: Int
				get() = nodes().asIterable().count()
			
			override fun contains(element: N) =
				nodes().asIterable().any { it == element }
			
			override fun containsAll(elements: Collection<N>) =
				elements.all { contains(it) }
			
			override fun isEmpty() =
				this@LuaGraphImpl.isEmpty()
			
			override fun iterator() =
				nodes()
		}
	
	override val edges: Collection<E>
		get() = object : Collection<E> {
			override val size: Int
				get() = nodes().asIterable().count() - 1
			
			override fun contains(element: E) =
				hasEdge(delegate.getEdgeFrom(element), delegate.getEdgeTo(element))
			
			override fun containsAll(elements: Collection<E>) =
				elements.all { contains(it) }
			
			override fun isEmpty(): Boolean {
				val first = head ?: return true
				return delegate.childrenOfNode(first).isEmpty()
			}
			
			override fun iterator() =
				iterator(method = null)?.map { it.edge } ?: emptyList<E>().iterator()
		}
	
	override fun filterSmoothing(
		predicate: (node: N) -> Boolean,
		newGetEdge: ((from: N, to: N) -> E)?,
		headAlternative: ((previousHead: N) -> N)?,
	): LuaGraph<N, E> {
		val headAlternativeDefaulted = headAlternative
			?: { delegate.childrenOfNode(it).singleOrNull() ?: error("head has multiple children") }
		
		var head: N = head ?: return this
		while(!predicate(head)) {
			head = headAlternativeDefaulted(head)
		}
		
		val delegate = object : Delegate<N, E> by delegate {
			override fun childrenOfNode(node: N): Collection<N> =
				delegate.childrenOfNode(node).mapNotNull {
					var next: N? = it
					while(next != null && !predicate(next)) {
						val candidates = delegate.childrenOfNode(next)
						next = when(candidates.size) {
							0 -> null
							1 -> candidates.first()
							else -> error("not supports node that both satisfies !predicate(node) and degree >= 2")
						}
					}
					next
				}
			
			override fun getEdge(from: N, to: N): E? =
				if(newGetEdge != null) newGetEdge(from, to) else delegate.getEdge(from, to)
		}
		
		return object : LuaGraphImpl<N, E>(head = head) {
			override val delegate = delegate
		}
	}
}
