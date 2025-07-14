package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockItem
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackModel
import com.simibubi.create.Create
import com.simibubi.create.AllTags.AllBlockTags as CreateBlockTags
import com.simibubi.create.content.trains.track.TrackBlockStateGenerator
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackModel
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.material.MapColor
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions
import java.util.function.Supplier


object AllBlocks {
	private val Registry = FlexiRegistry
	
	val FlexiTrack = Registry
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
