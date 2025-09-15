package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackBlock
import com.simibubi.create.AllDisplaySources
import com.simibubi.create.Create
import com.simibubi.create.api.behaviour.display.DisplaySource
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem
import com.simibubi.create.foundation.data.AssetLookup
import com.simibubi.create.foundation.data.BlockStateGen
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.RenderType
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.material.MapColor
import java.util.function.Supplier
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserver as AdvancedTrackObserverPoint
import com.simibubi.create.AllTags as CreateTags


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
		.model { c, p -> p.blockItem(c, "/block") }
		.build()
		.register()
	
	
	val MiddleTrack: BlockEntry<MiddleTrackBlock> = Registry
		.block("middle_track", ::MiddleTrackBlock)
		.properties {
			it.mapColor(MapColor.METAL)
				.noCollission()
				.noOcclusion()
				.replaceable()
		}
		.blockstate { c, p -> p.simpleBlock(c.get(), p.models().withExistingParent(c.name, p.mcLoc("block/stone"))) }
		.lang("Middle Track Block")
		.register()
	
	@Suppress("DEPRECATION", "removal")
	val FlexiTrack: BlockEntry<FlexiTrackBlock> = Registry
		.block("flexi_track") { FlexiTrackBlock(it, FlexiTrackMaterial.Andesite) }
		.initialProperties(SharedProperties::stone)
		.properties {
			it.mapColor(MapColor.METAL)
				.strength(.8f)
				.sound(SoundType.METAL)
				.noOcclusion()
				.forceSolidOn()
		}
		.blockstate { c, p ->
			p.simpleBlock(
				c.entry,
				p.models().withExistingParent(c.name, Create.asResource("block/track/x_ortho"))
			)
		}
		.addLayer { Supplier { RenderType.cutoutMipped() } }
		.clientExtension { -> Supplier { FlexiTrackBlock.RenderProperties() } }
		.tag(AllTags.Features.FlexiTrack.block)
		.tag(CreateTags.AllBlockTags.TRACKS.tag)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(CreateTags.AllBlockTags.RELOCATION_NOT_SUPPORTED.tag)
		.tag(CreateTags.AllBlockTags.TRACKS.tag)
		.tag(CreateTags.AllBlockTags.GIRDABLE_TRACKS.tag)
		.lang("Flexi Train Track")
		.item(::FlexiTrackBlockItem)
		.tag(CreateTags.AllItemTags.TRACKS.tag)
		.model { c, p -> p.generated(c, Create.asResource("item/track")) }
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
