package com.lhwdev.minecraft.railx

import com.lhwdev.minecraft.railx.peripherals.display.source.ComputerizedDisplaySourceBlock
import com.lhwdev.minecraft.railx.peripherals.display.source.ComputerizedDisplaySourceBlockEntity
import com.lhwdev.minecraft.railx.peripherals.display.target.ComputerizedDisplayTargetBlock
import com.lhwdev.minecraft.railx.peripherals.display.target.ComputerizedDisplayTargetBlockEntity
import com.lhwdev.minecraft.railx.peripherals.redstoneLink.ComputerizedRedstoneLinkBlock
import com.lhwdev.minecraft.railx.peripherals.redstoneLink.ComputerizedRedstoneLinkBlockEntity
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.TrainNetworkObserverBlock
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.TrainNetworkObserverBlockEntity
import com.lhwdev.minecraft.railx.peripherals.trains.networkObserver.TrainNetworkObserverRenderer
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem
import com.tterrag.registrate.util.nullness.NonNullBiFunction
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import java.util.function.Supplier
import net.minecraft.core.registries.Registries as MinecraftRegistries

object Registries {
	val ITEM_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, RailX.modId)
	val BLOCK_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, RailX.modId)
	val TILE_REGISTRY = DeferredRegister.create(
		ForgeRegistries.BLOCK_ENTITY_TYPES,
		RailX.modId
	)
	val CREATIVE_TABS_REGISTRY =
		DeferredRegister.create(MinecraftRegistries.CREATIVE_MODE_TAB, RailX.modId)
	
	
	fun registerBlock(name: String, blockSupplier: () -> Block): RegistryObject<Block> {
		RailX.logger.info("Queuing block: $name")
		return BLOCK_REGISTRY.register(name, blockSupplier)
	}
	
	@JvmOverloads
	fun registerBlockItem(
		name: String,
		block: RegistryObject<Block>,
		properties: Item.Properties = Item.Properties(),
		function: NonNullBiFunction<in Block, Item.Properties, out BlockItem> =
			NonNullBiFunction { bl, props -> BlockItem(bl, props) },
	): RegistryObject<Item> {
		RailX.logger.info("Queuing block item: $name")
		return ITEM_REGISTRY.register(name) { function.apply(block.get(), properties) }
	}
	
	fun <T : BlockEntity> registerBlockEntity(
		name: String,
		supplier: BlockEntitySupplier<T>,
		vararg blocks: Supplier<Block>,
	): RegistryObject<BlockEntityType<T>> {
		RailX.logger.info("Queuing tile: $name")
		return TILE_REGISTRY.register(name) {
			val rBlocks = blocks.map { it.get() }.toTypedArray<Block>()
			
			@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
			BlockEntityType.Builder.of(supplier, *rBlocks).build(null)
		}
	}
	
	// Computerized Display Source
	var COMPUTERIZED_DISPLAY_SOURCE = registerBlock(
		"computerized_display_source"
	) { ComputerizedDisplaySourceBlock() }
	var COMPUTERIZED_DISPLAY_SOURCE_ITEM = registerBlockItem(
		"computerized_display_source",
		COMPUTERIZED_DISPLAY_SOURCE,
	)
	var COMPUTERIZED_DISPLAY_SOURCE_BLOCK_ENTITY = registerBlockEntity(
		"computerized_display_source",
		{ pos, state -> ComputerizedDisplaySourceBlockEntity(pos, state) },
		COMPUTERIZED_DISPLAY_SOURCE
	)
	
	// Computerized Display Target
	var COMPUTERIZED_DISPLAY_TARGET = registerBlock(
		"computerized_display_target"
	) { ComputerizedDisplayTargetBlock() }
	var COMPUTERIZED_DISPLAY_TARGET_ITEM = registerBlockItem(
		"computerized_display_target",
		COMPUTERIZED_DISPLAY_TARGET,
	)
	var COMPUTERIZED_DISPLAY_TARGET_BLOCK_ENTITY = registerBlockEntity(
		"computerized_display_target",
		{ pos, state -> ComputerizedDisplayTargetBlockEntity(pos, state) },
		COMPUTERIZED_DISPLAY_TARGET
	)
	
	// Computerized Redstone Link
	var COMPUTERIZED_REDSTONE_LINK = registerBlock(
		"computerized_redstone_link"
	) { ComputerizedRedstoneLinkBlock() }
	var COMPUTERIZED_REDSTONE_LINK_ITEM = registerBlockItem(
		"computerized_redstone_link",
		COMPUTERIZED_REDSTONE_LINK,
	)
	var COMPUTERIZED_REDSTONE_LINK_BLOCK_ENTITY = registerBlockEntity(
		"computerized_redstone_link",
		{ pos, state -> ComputerizedRedstoneLinkBlockEntity(pos, state) },
		COMPUTERIZED_REDSTONE_LINK
	)
	
	// Train Network Observer
	var TRAIN_NETWORK_OBSERVER = registerBlock(
		"train_network_observer"
	) { TrainNetworkObserverBlock() }
	var TRAIN_NETWORK_OBSERVER_ITEM = registerBlockItem(
		"train_network_observer",
		TRAIN_NETWORK_OBSERVER,
		Item.Properties(),
		TrackTargetingBlockItem.ofType(TrainNetworkObserverBlockEntity.NETWORK_OBSERVER)
	)
	var TRAIN_NETWORK_OBSERVER_BLOCK_ENTITY = registerBlockEntity(
		"train_network_observer",
		{ pos, state -> TrainNetworkObserverBlockEntity(pos, state) },
		TRAIN_NETWORK_OBSERVER
	)
	
	
	val TAB: RegistryObject<CreativeModeTab> = CREATIVE_TABS_REGISTRY.register("railx.tab") {
		CreativeModeTab.builder().apply {
			icon { ItemStack(COMPUTERIZED_DISPLAY_TARGET_ITEM.get()) }
			displayItems { params, output ->
				output.acceptAll(
					listOf(
						COMPUTERIZED_DISPLAY_SOURCE_ITEM,
						COMPUTERIZED_DISPLAY_TARGET_ITEM,
						COMPUTERIZED_REDSTONE_LINK_ITEM,
						TRAIN_NETWORK_OBSERVER_ITEM,
					).map { ItemStack(it.get()) }
				)
			}
		}.build()
	}
	
	// Real loading
	fun init(modEventBus: IEventBus) {
		RailX.logger.info("Registering all registries.")
		BLOCK_REGISTRY.register(modEventBus)
		ITEM_REGISTRY.register(modEventBus)
		TILE_REGISTRY.register(modEventBus)
		CREATIVE_TABS_REGISTRY.register(modEventBus)
		RailX.logger.info("Registered all registries.")
	} // For loading.
	
	// Events
	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	object RegistryEvents {
		//		@SubscribeEvent
		//		fun onCreativeModeTabRegister(event: BuildCreativeModeTabContentsEvent) {
		//
		//		}
		
		@SubscribeEvent
		fun fmlCommon(blockRegistryEvent: FMLCommonSetupEvent) {
			RailX.logger.info("Registering all Create behaviours.")
			registerBehaviors()
			RailX.logger.info("Registered all Create behaviour.")
		}
		
		//		@SubscribeEvent
		//		fun fmlClient(blockRegistryEvent: FMLClientSetupEvent) {
		//			ItemBlockRenderTypes.setRenderLayer(
		//				COMPUTERIZED_DISPLAY_TARGET.get(),
		//				RenderType.cutout()
		//			)
		//			ItemBlockRenderTypes.setRenderLayer(
		//				COMPUTERIZED_REDSTONE_LINK.get(),
		//				RenderType.cutout()
		//			)
		//		}
	}
	
	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
	object RegistryClientEvents {
		@SubscribeEvent
		fun modRenderer(event: RegisterRenderers) {
			event.registerBlockEntityRenderer(
				TRAIN_NETWORK_OBSERVER_BLOCK_ENTITY.get()
			) { context ->
				TrainNetworkObserverRenderer(context)
			}
		}
	}
}
