package com.lhwdev.minecraft.railx.flexiTrack

import com.mojang.serialization.MapCodec
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackShape
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property


// private fun FlexiBlockState.withBlockEntity(fn: (be: FlexiTrackBlockEntity) -> Unit): FlexiBlockState =
// 	onSet { level, pos, state ->
// 		val be = level.getBlockEntity(pos) as FlexiTrackBlockEntity
// 		fn(be)
// 		this
// 	}
//
// fun FlexiBlockState.setShape(shape: FlexiShape): FlexiBlockState =
// 	withBlockEntity { it.shape = shape }
//
// fun FlexiBlockState.mapShape(fn: (FlexiShape) -> FlexiShape) =
// 	withBlockEntity { it.shape = fn(it.shape) }

inline fun FlexiBlockState.mapShape(crossinline fn: (FlexiShape) -> FlexiShape): FlexiBlockState =
	setShape(fn(shape))


class FlexiBlockState(
	block: Block,
	values: Reference2ObjectArrayMap<Property<*>, Comparable<*>>,
	propertiesCodec: MapCodec<BlockState>,
	val shape: FlexiShape,
	val base: FlexiBlockState?
) : BlockState(block, values, propertiesCodec) {
	@PublishedApi
	internal fun setShape(shape: FlexiShape): FlexiBlockState = FlexiBlockState(
		block = block,
		values = values as Reference2ObjectArrayMap<Property<*>, Comparable<*>>,
		propertiesCodec = propertiesCodec,
		shape = shape,
		base = base ?: this,
	)
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : Comparable<T>> getValue(property: Property<T>): T = when(property) {
		TrackBlock.HAS_BE -> false.also { Error("HAS_BE access").printStackTrace() }
		TrackBlock.SHAPE -> TrackShape.NONE.also { Error("SHAPE access").printStackTrace() }
		
		else -> super.getValue(property)
	} as T
	
	override fun <T : Comparable<T>, V : T> setValue(
		property: Property<T>, value: V,
	): FlexiBlockState = when(property) {
		TrackBlock.HAS_BE -> this
		
		else -> super.setValue(property, value)
	} as FlexiBlockState
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is FlexiBlockState -> false
		else -> shape == other.shape && values === other.values
	}
	
	override fun hashCode(): Int = shape.hashCode() * 31 + values.hashCode()
}
//
// class FlexiBlockState(
// 	block: Block,
// 	values: Reference2ObjectArrayMap<Property<*>, Comparable<*>>,
// 	propertiesCodec: MapCodec<BlockState>,
// 	private val onSetFn: ((level: LevelReader, pos: BlockPos, state: FlexiBlockState) -> FlexiBlockState)? = null,
// ) : BlockState(block, values, propertiesCodec) {
// 	fun onSet(mapping: (level: LevelReader, pos: BlockPos, state: FlexiBlockState) -> FlexiBlockState): FlexiBlockState =
// 		FlexiBlockState(
// 			block = block,
// 			values = values as Reference2ObjectArrayMap<Property<*>, Comparable<*>>,
// 			propertiesCodec = propertiesCodec,
// 			onSetFn = if(onSetFn == null) {
// 				mapping
// 			} else {
// 				{ level, pos, state -> mapping(level, pos, onSetFn(level, pos, state)) }
// 			},
// 		)
//
// 	fun setStateOnLevel(level: LevelReader, pos: BlockPos): BlockState = if(onSetFn == null) {
// 		this
// 	} else {
// 		onSetFn(level, pos, this)
// 	}
//
// 	@Suppress("UNCHECKED_CAST")
// 	override fun <T : Comparable<T>> getValue(property: Property<T>): T = when(property) {
// 		TrackBlock.HAS_BE -> false.also { Error("HAS_BE access").printStackTrace() }
// 		TrackBlock.SHAPE -> TrackShape.NONE.also { Error("SHAPE access").printStackTrace() }
//
// 		else -> super.getValue(property)
// 	} as T
//
// 	override fun <T : Comparable<T>, V : T> setValue(
// 		property: Property<T>, value: V,
// 	): FlexiBlockState = when(property) {
// 		TrackBlock.HAS_BE -> this
//
// 		else -> super.setValue(property, value)
// 	} as FlexiBlockState
// }
