package com.lhwdev.minecraft.railx.registry

import com.google.common.collect.ImmutableSet
import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.mixin.common.BlockEntityTypeAccessor
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.data.CreateRegistrate
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.builders.BlockEntityBuilder
import com.tterrag.registrate.builders.ItemBuilder
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.RegisterEvent


val RailXRegistry = RailXRegistrate(RailX.Id)


@Suppress("UNCHECKED_CAST")
class RailXRegistrate(modId: String) : CreateRegistrate(modId) {
	init {
		@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		defaultCreativeTab(null as ResourceKey<CreativeModeTab>?)
	}
	
	fun location(name: String): ResourceLocation =
		ResourceLocation(modid, name)
	
	@Suppress("UNCHECKED_CAST")
	fun <T> registryOf(key: ResourceKey<Registry<T>>): Registry<T> =
		BuiltInRegistries.REGISTRY[key.location()] as Registry<T>
	
	
	val allBlocks: List<BlockEntry<*>>
		@Suppress("UNCHECKED_CAST")
		get() = getAll(Registries.BLOCK) as List<BlockEntry<*>>
	
	
	inline fun <T : Block> block(
		name: String,
		factory: NonNullFunction<BlockBehaviour.Properties, T>,
		block: BlockBuilder<T, RailXRegistrate>.() -> Unit,
	): BlockEntry<T> = (block(name, factory) as BlockBuilder<T, RailXRegistrate>)
		.apply(block)
		.register()
	
	fun <T : BlockEntity> blockEntity(
		name: String,
		factory: BlockEntityBuilder.BlockEntityFactory<T>,
		block: RailXBlockEntityBuilder<T, RailXRegistrate>.() -> Unit,
	): RailXBlockEntityEntry<T> =
		(entry(name) { callback -> RailXBlockEntityBuilder(this, this, name, callback, factory) }
			as RailXBlockEntityBuilder<T, RailXRegistrate>)
			.apply(block)
			.register() as RailXBlockEntityEntry<T>
	
	
	override fun getModEventBus(): IEventBus =
		RailX.bus
	
	private val addValidToBlockEntity = mutableListOf<BlockBuilder<out Block, *>>()
	fun <T> addValidToBlockEntity(block: BlockBuilder<T, *>) where T : Block, T : IBE<*> {
		addValidToBlockEntity += block
	}
	
	override fun onRegisterLate(event: RegisterEvent) {
		super.onRegisterLate(event)
		if(event.registryKey != Registries.BLOCK_ENTITY_TYPE) return
		
		for(entry in addValidToBlockEntity) {
			val block = entry.entry
			val accessor = (block as IBE<*>).blockEntityType as BlockEntityTypeAccessor
			accessor.validBlocks = ImmutableSet.Builder<Block>()
				.addAll(accessor.validBlocks)
				.add(block)
				.build()
		}
	}
}


inline fun <B : Block, I : Item, P> BlockBuilder<B, P>.item(
	crossinline factory: (B, Item.Properties) -> I,
	builder: ItemBuilder<I, BlockBuilder<B, P>>.() -> Unit,
) {
	item { block, properties -> factory(block, properties) }
		.apply(builder).build()
}

fun <T : Block, P> BlockBuilder<T, P>.validFor(blockEntity: RailXBlockEntityEntry<*>): BlockBuilder<T, P> =
	onRegister { block -> blockEntity.validBlock { block } }

fun <T> BlockBuilder<T, out RailXRegistrate>.addValidToBlockEntity() where T : Block, T : IBE<*> {
	parent.addValidToBlockEntity(block = this)
}
