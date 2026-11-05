package com.lhwdev.minecraft.railx.registry.registrate

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.compat.ConditionalBlockLootProvider
import com.lhwdev.minecraft.railx.registry.RailXBlockEntityBuilder
import com.lhwdev.minecraft.railx.registry.RailXBlockEntityEntry
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.data.CreateRegistrate
import com.tterrag.registrate.builders.*
import com.tterrag.registrate.providers.ProviderType
import com.tterrag.registrate.providers.RegistrateDataProvider
import com.tterrag.registrate.providers.RegistrateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.entry.RegistryEntry
import com.tterrag.registrate.util.nullness.NonNullConsumer
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.data.event.GatherDataEvent
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent
import java.util.*


val RailXRegistry = RailXRegistrate(RailX.Id)


@Suppress("UNCHECKED_CAST")
class RailXRegistrate(modId: String) : CreateRegistrate(modId) {
	init {
		@Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		defaultCreativeTab(null as ResourceKey<CreativeModeTab>?)
	}
	
	fun location(name: String): ResourceLocation =
		ResourceLocation.fromNamespaceAndPath(modid, name)
	
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
	
	fun <T> dataComponentType(
		name: String,
		block: DataComponentType.Builder<T>.() -> Unit,
	): RegistryEntry<DataComponentType<*>, DataComponentType<T>> =
		generic(name, Registries.DATA_COMPONENT_TYPE) { DataComponentType.builder<T>().apply(block).build() }
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
	
	
	/// Compat resource dependencies
	
	private class CompatResource(val dependencyModId: String)
	
	private val compatResources = mutableMapOf<ProviderType<*>, MutableMap<String, CompatResource>>()
	private var compatData: RegistrateDataProvider? = null
	
	fun registerCompat(registry: ProviderType<*>, name: String, dependencyModId: String) {
		compatResources.getOrPut(registry) { mutableMapOf() }
			.put(name, CompatResource(dependencyModId))
	}
	
	fun registerCompatBlock(name: String, dependencyModId: String, itemName: String? = name) {
		registerCompat(registry = ProviderType.BLOCK_TAGS, name, dependencyModId)
		if(itemName != null) registerCompat(registry = ProviderType.ITEM_TAGS, itemName, dependencyModId)
	}
	
	override fun <P : RegistrateProvider, R> setDataGenerator(
		builder: Builder<R, *, *, *>,
		type: ProviderType<out P>,
		cons: NonNullConsumer<out P>,
	): RailXRegistrate {
		val resources = compatResources.getOrDefault(type, emptyMap())
		if(builder.name in resources) {
			(builder as? AbstractBuilder<*, *, *, *>)?.asOptional()
		}
		
		return super.setDataGenerator(builder, type, cons) as RailXRegistrate
	}
	
	override fun onData(event: GatherDataEvent) {
		val provider = RegistrateDataProvider(this, modid, event)
		compatData = provider
		
		val conditionalBlockLootProvider = ConditionalBlockLootProvider(
			delegate = provider,
			dataRoot = event.generator.packOutput.outputFolder.resolve("data"),
			blockDependencies = compatResources.getOrDefault(ProviderType.BLOCK_TAGS, emptyMap())
				.entries.associate { (k, v) -> location(k) to v.dependencyModId },
		)
		event.generator.addProvider(true, conditionalBlockLootProvider)
	}
	
	override fun <P : RegistrateProvider> getDataProvider(type: ProviderType<P>): Optional<P> =
		compatData?.getSubProvider(type) ?: super.getDataProvider(type)
	
	
	/// addValidToBlockEntity
	
	private val addValidToBlockEntity = mutableListOf<BlockBuilder<out Block, *>>()
	fun <T> addValidToBlockEntity(block: BlockBuilder<T, *>) where T : Block, T : IBE<*> {
		addValidToBlockEntity += block
	}
	
	
	override fun registerEventListeners(bus: IEventBus): RailXRegistrate {
		bus.addListener(BlockEntityTypeAddBlocksEvent::class.java) { event ->
			for(entry in addValidToBlockEntity) {
				val block = entry.entry
				event.modify((block as IBE<*>).blockEntityType, block)
			}
		}
		return super.registerEventListeners(bus) as RailXRegistrate
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
