@file:Suppress("unused")

package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlock
import com.lhwdev.minecraft.railx.compat.flexiTrack.FlexiTrackMaterialCompat
import com.lhwdev.minecraft.railx.compat.flexiTrack.railways.TestCustomTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackBlock
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphTrackBlock
import com.lhwdev.minecraft.railx.splitGraph.flexiBlock.FlexiSplitGraphTrackBlock
import com.railwayteam.railways.Railways
import com.railwayteam.railways.registry.CRTags
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.AllDisplaySources
import com.simibubi.create.Create
import com.simibubi.create.api.behaviour.display.DisplaySource
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.data.AssetLookup
import com.simibubi.create.foundation.data.BlockStateGen
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.builders.ItemBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.RenderType
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import java.util.function.Supplier
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserver as AdvancedTrackObserverPoint
import com.simibubi.create.AllTags as CreateTags


object AllBlocks {
	private val Registry = RailXRegistry
	
	fun register() {}
	
	val AdvancedTrackObserver = Registry.block("advanced_track_observer", ::AdvancedTrackObserverBlock) {
		initialProperties(SharedProperties::softMetal)
		properties {
			it.mapColor(MapColor.PODZOL)
				.noOcclusion()
				.sound(SoundType.NETHERITE_BLOCK)
		}
		blockstate { c, p -> BlockStateGen.simpleBlock(c, p, AssetLookup.forPowered(c, p)) }
		tag(BlockTags.MINEABLE_WITH_PICKAXE)
		transform(DisplaySource.displaySource(AllDisplaySources.OBSERVED_TRAIN_NAME))
		lang("Advanced Track Observer")
		item(TrackTargetingBlockItem.ofType(AdvancedTrackObserverPoint.ObserverEdgePointType))
			.model { c, p -> p.blockItem(c, "/block") }
			.build()
	}
	
	
	val MiddleTrack = Registry.block("middle_track", ::MiddleTrackBlock) {
		properties {
			it.mapColor(MapColor.METAL)
				.noCollission()
				.noOcclusion()
				.replaceable()
		}
		blockstate { c, p -> p.simpleBlock(c.get(), p.models().withExistingParent(c.name, p.mcLoc("block/stone"))) }
		lang("Middle Track Block")
	}
	
	val FlexiTrack = Registry.flexiTrackBlock(
		name = "flexi_track",
		material = FlexiTrackMaterial.Andesite,
		factory = ::FlexiTrackBlock,
	) {
		tag(AllTags.Features.FlexiTrack.block)
		lang("Flexi Train Track")
	}
	
	
	val SplitGraphTrack: BlockEntry<SplitGraphTrackBlock> = Registry.trackBlock(
		name = "split_graph_track",
		material = TrackMaterial.ANDESITE,
		factory = ::SplitGraphTrackBlock,
		blockStates = TrackBlockStateGenerator()::generate,
		itemFactory = ::TrackBlockItem,
	) {
		tag(AllTags.Features.SplitGraph.block)
		lang("Split Graph Train Track")
	}
	
	val FlexiSplitGraphTrack: BlockEntry<FlexiSplitGraphTrackBlock> = Registry.flexiTrackBlock(
		name = "flexi_split_graph_track",
		material = FlexiTrackMaterial.Andesite,
		factory = ::FlexiSplitGraphTrackBlock,
	) {
		tag(AllTags.Features.SplitGraph.block)
		lang("Flexible Split Graph Train Track")
	}
	
	
	val WideAndesiteFlexiTrack: BlockEntry<TestCustomTrackBlock> = Registry.flexiTrackBlock(
		name = "wide_andesite_flexi_track",
		material = FlexiTrackMaterialCompat.WideAndesite,
		blockStates = { c, p ->
			val name = "create_andesite_wide"
			p.simpleBlock(c.entry, p.models().withExistingParent(c.name, Railways.asResource("block/track/$name/x_ortho")))
		},
		factory = ::TestCustomTrackBlock,
	) {
		lang("Wide Flexible Train Track")
	}
}


@Suppress("DEPRECATION", "removal")
private inline fun <Track, I : Item, Material : TrackMaterial> RailXRegistrate.trackBlock(
	name: String,
	material: Material,
	crossinline factory: (BlockBehaviour.Properties, Material) -> Track,
	crossinline blockStates: (DataGenContext<Block, Track>, RegistrateBlockstateProvider) -> Unit,
	crossinline itemFactory: (Track, Item.Properties) -> I,
	itemBuilder: ItemBuilder<I, BlockBuilder<Track, RailXRegistrate>>.() -> Unit = {
		model { c, p -> p.generated(c, Create.asResource("item/track")) }
	},
	builder: BlockBuilder<Track, RailXRegistrate>.() -> Unit,
): BlockEntry<Track> where Track : Block, Track : ITrackBlock = block(name, { factory(it, material) }) {
	initialProperties(SharedProperties::stone)
	properties {
		it.mapColor(MapColor.METAL)
			.strength(.8f)
			.sound(SoundType.METAL)
			.noOcclusion()
			.forceSolidOn()
	}
	blockstate { c, p -> blockStates(c, p) }
	addLayer { Supplier { RenderType.cutoutMipped() } }
	tag(CreateTags.AllBlockTags.TRACKS.tag)
	tag(BlockTags.MINEABLE_WITH_PICKAXE)
	tag(CreateTags.AllBlockTags.RELOCATION_NOT_SUPPORTED.tag)
	tag(CreateTags.AllBlockTags.TRACKS.tag)
	if(material.trackType != CRTrackMaterials.CRTrackType.MONORAIL)
		tag(CreateTags.AllBlockTags.GIRDABLE_TRACKS.tag)
	
	item(factory = { block, properties -> itemFactory(block, properties) }) {
		tag(CreateTags.AllItemTags.TRACKS.tag)
		model { c, p -> p.generated(c, Create.asResource("item/track")) }
		if(
			material == CRTrackMaterials.PHANTOM ||
			material == CRTrackMaterials.getWide(CRTrackMaterials.PHANTOM) ||
			material == CRTrackMaterials.getNarrow(CRTrackMaterials.PHANTOM)
		) tag(CRTags.AllItemTags.PHANTOM_TRACK_REVEALING.tag)
		itemBuilder()
	}
	
	builder()
}

private inline fun <Track> RailXRegistrate.flexiTrackBlock(
	name: String,
	material: FlexiTrackMaterial,
	crossinline factory: (BlockBehaviour.Properties, FlexiTrackMaterial) -> Track,
	crossinline blockStates: (DataGenContext<Block, Track>, RegistrateBlockstateProvider) -> Unit = { c, p ->
		p.simpleBlock(c.entry, p.models().withExistingParent(c.name, Create.asResource("block/track/x_ortho")))
	},
	itemBuilder: ItemBuilder<FlexiTrackBlockItem, BlockBuilder<Track, RailXRegistrate>>.() -> Unit = {
		model { c, p -> p.generated(c, Create.asResource("item/track")) }
	},
	builder: BlockBuilder<Track, RailXRegistrate>.() -> Unit,
): BlockEntry<Track> where Track : Block, Track : ITrackBlock =
	trackBlock(name, material, factory, blockStates, itemFactory = ::FlexiTrackBlockItem, itemBuilder, builder)
