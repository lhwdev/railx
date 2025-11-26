package com.lhwdev.minecraft.railx.flexiTrack

import com.google.common.collect.ImmutableMap
import com.mojang.serialization.MapCodec
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property


inline fun FlexiBlockState.mapState(crossinline fn: (FlexiState) -> FlexiState): FlexiBlockState = when(this) {
	is FlexiBlockState.Base -> FlexiBlockState.Update(base = this, stateFn = { fn(it) })
	is FlexiBlockState.Update -> FlexiBlockState.Update(base, stateFn = { fn(stateFn(it)) })
}

inline fun FlexiBlockState.mapShape(crossinline fn: (FlexiShape) -> FlexiShape): FlexiBlockState =
	mapState { it.copy(baseShape = fn(it.baseShape)) }


/**
 * **Only exists for `Block.rotate()` and `Block.mirror()`...**
 */
sealed class FlexiBlockState(
	block: FlexiTrackBlock,
	values: ImmutableMap<Property<*>, Comparable<*>>,
	propertiesCodec: MapCodec<BlockState>,
) : BlockState(block, values, propertiesCodec) {
	companion object {
		fun create(
			block: FlexiTrackBlock,
			values: ImmutableMap<Property<*>, Comparable<*>>,
			propertiesCodec: MapCodec<BlockState>,
		): FlexiBlockState = Base(block, values, propertiesCodec)
	}
	
	abstract val base: Base
	
	open fun mapState(previous: FlexiState): FlexiState = previous
	
	class Base internal constructor(
		block: FlexiTrackBlock,
		values: ImmutableMap<Property<*>, Comparable<*>>,
		propertiesCodec: MapCodec<BlockState>,
	) : FlexiBlockState(block, values, propertiesCodec) {
		override val base: Base
			get() = this
	}
	
	class Update(override val base: Base, val stateFn: (FlexiState) -> FlexiState) : FlexiBlockState(
		block = base.block,
		values = base.values as ImmutableMap<Property<*>, Comparable<*>>,
		propertiesCodec = base.propertiesCodec,
	) {
		init {
			System.err.println("railx:state FlexiBlockState.Update constructed; ${Error().stackTraceToString()}")
		}
		
		override fun mapState(previous: FlexiState): FlexiState =
			stateFn(previous)
		
		override fun equals(other: Any?): Boolean = when {
			this === other -> true
			other !is Update -> false
			else -> base == other.base && stateFn == other.stateFn
		}
		
		override fun hashCode(): Int =
			base.hashCode() * 31 + stateFn.hashCode()
	}
	
	override fun getBlock(): FlexiTrackBlock = super.getBlock() as FlexiTrackBlock
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : Comparable<T>> getValue(property: Property<T>): T = when(property) {
		TrackBlock.HAS_BE -> true
		TrackBlock.SHAPE -> TrackShape.NONE.also { Error("SHAPE access").printStackTrace() }
		
		else -> super.getValue(property)
	} as T
	
	override fun <T : Comparable<T>, V : T> setValue(
		property: Property<T>, value: V,
	): FlexiBlockState = when(property) {
		TrackBlock.HAS_BE -> this
		
		else -> super.setValue(property, value)
	} as FlexiBlockState
}
