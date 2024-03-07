package com.lhwdev.minecraft.railx.peripherals.common

import com.lhwdev.minecraft.railx.api.lua.LuaVararg
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaProperty


@LuaObject
interface LuaGraphIterable<N : Any, E : Any> {
	@LuaFunction
	fun iterator(method: LuaGraphIterator.Method? = null): LuaGraphIterator<N, E>?
}


@LuaObject
interface LuaGraph<N : Any, E : Any> : LuaGraphIterable<N, E>, Collection<N> {
	@LuaProperty
	val head: N?
	
	@LuaFunction
	fun hasNode(node: N): Boolean
	
	@LuaFunction
	fun getEdge(from: N, to: N): E?
	
	@LuaFunction
	fun hasEdge(from: N, to: N): Boolean
	
	
	@LuaProperty
	val nodes: Collection<N>
	
	@LuaProperty
	val edges: Collection<E>
	
	
	/**
	 * Note: see 'smoothing out' in https://en.wikipedia.org/wiki/Homeomorphism_(graph_theory)#Subdivisions for more
	 * detail about smoothing in graph theory.
	 */
	@LuaFunction
	fun filterSmoothing(
		predicate: (node: N) -> Boolean,
		newGetEdge: ((from: N, to: N) -> E)? = null,
		headAlternative: ((previousHead: N) -> N)? = null,
	): LuaGraph<N, E>
}


@LuaObject
interface LuaGraphIterator<N : Any, E : Any> : Iterator<LuaGraphIterator.Next<N, E>> {
	enum class Method { Dfs, Bfs }
	
	data class Next<N, E>(val node: N, val edge: E) : LuaVararg {
		override val values: Array<Any?>
			get() = arrayOf(node, edge)
	}
	
	
	fun nextNode(): N
	
	
	@LuaProperty
	val current: N
	
	@LuaFunction
	fun iterateFromHere(method: Method? = null): LuaGraphIterator<N, E>
	
	@LuaFunction
	fun toGraph(): LuaGraph<N, E>
}

