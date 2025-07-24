package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserver as AdvancedTrackObserverPoint
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackModel
import com.simibubi.create.AllDisplaySources
import com.simibubi.create.Create
import com.simibubi.create.api.behaviour.display.DisplaySource
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem
import com.simibubi.create.foundation.data.AssetLookup
import com.simibubi.create.foundation.data.BlockStateGen
import com.simibubi.create.foundation.data.ModelGen
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.material.MapColor
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions
import java.util.function.Supplier
import com.simibubi.create.AllTags.AllBlockTags as CreateBlockTags


object AllBlocks {
	private val Registry = RailXRegistry
	
	val AdvancedTrackObserver: BlockEntry<AdvancedTrackObserverBlock> = Registry
		.block("advanced_track_observer", ::AdvancedTrackObserverBlock)
		.initialProperties(SharedProperties::softMetal)
		.properties {
			it.mapColor(MapColor.PODZOL)
				.noOcclusion()
				.sound(SoundType.NETHERITE_BLOCK)
		}
		.blockstate { c, p -> BlockStateGen.simpleBlock(c, p, AssetLookup.forPowered(c, p)) }
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.transform(DisplaySource.displaySource(AllDisplaySources.OBSERVED_TRAIN_NAME))
		.lang("Advanced Track Observer")
		.item(TrackTargetingBlockItem.ofType(AdvancedTrackObserverPoint.ObserverEdgePointType))
		.transform(ModelGen.customItemModel())
		.register()
	
	val FlexiTrack: BlockEntry<FlexiTrackBlock> = Registry
		.block("flexi_track") { FlexiTrackBlock(it, TrackMaterial.ANDESITE) }
		.initialProperties(SharedProperties::stone)
		.properties {
			it.mapColor(MapColor.METAL)
				.strength(.8f)
				.sound(SoundType.METAL)
				.noOcclusion()
				.forceSolidOn()
		}
		.clientExtension { -> Supplier { FlexiTrackBlock.RenderProperties() as IClientBlockExtensions } }
		// .onRegister(CreateRegistrate.blockModel { NonNullFunction { FlexiTrackModel(it) } })
		.blockstate(FlexiTrackModel::registerBlockState)
		.tag(AllTags.Features.FlexiTrack.block)
		.tag(CreateBlockTags.TRACKS.tag)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(CreateBlockTags.RELOCATION_NOT_SUPPORTED.tag)
		.tag(CreateBlockTags.TRACKS.tag)
		.tag(CreateBlockTags.GIRDABLE_TRACKS.tag)
		.lang("Train Track")
		.item(::FlexiTrackBlockItem)
		.model { c, p -> p.generated(c, Create.asResource("item/${c.name}")) }
		.build()
		.register()
	
	
	/* REGISTRATE.block("track", TrackMaterial.ANDESITE::createBlock)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.METAL)
			.strength(0.8F)
			.sound(SoundType.METAL)
			.noOcclusion()
			.forceSolidOn())
		.addLayer(() -> RenderType::cutoutMipped)
		.transform(pickaxeOnly())
		.clientExtension(() -> () -> new TrackBlock.RenderProperties())
		.onRegister(CreateRegistrate.blockModel(() -> TrackModel::new))
		.blockstate(new TrackBlockStateGenerator()::generate)
		.tag(AllBlockTags.RELOCATION_NOT_SUPPORTED.tag)
		.tag(AllBlockTags.TRACKS.tag)
		.tag(AllBlockTags.GIRDABLE_TRACKS.tag)
		.lang("Train Track")
		.item(TrackBlockItem::new)
		.tag(AllItemTags.TRACKS.tag)
		.model((c, p) -> p.generated(c, Create.asResource("item/" + c.getName())))
		.build()
		.register(); */
	
}
