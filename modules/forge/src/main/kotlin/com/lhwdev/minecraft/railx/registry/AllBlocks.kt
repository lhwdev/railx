@file:Suppress("unused")

package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.advancedRoller.AdvancedRollerBlock
import com.lhwdev.minecraft.railx.advancedRoller.AdvancedRollerMovementBehavior
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserverBlock
import com.lhwdev.minecraft.railx.common.gravelLayer.GravelLayerBlock
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackBlock
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphTrackBlock
import com.lhwdev.minecraft.railx.splitGraph.flexiBlock.FlexiSplitGraphTrackBlock
import com.railwayteam.railways.registry.CRTags
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.AllDisplaySources
import com.simibubi.create.Create
import com.simibubi.create.api.behaviour.display.DisplaySource
import com.simibubi.create.api.behaviour.movement.MovementBehaviour
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockItem
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.data.AssetLookup
import com.simibubi.create.foundation.data.BlockStateGen
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import net.neoforged.neoforge.client.model.generators.ConfiguredModel
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Supplier
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserver as AdvancedTrackObserverPoint
import com.simibubi.create.AllBlocks as CreateBlocks
import com.simibubi.create.AllTags as CreateTags


object AllBlocks {
	private val Registry = RailXRegistry
	
	fun register() {}
	
	
	val GravelLayer = Registry.block("gravel_layer", ::GravelLayerBlock) {
		initialProperties(Blocks::GRAVEL)
		blockstate { c, p ->
			val gravel = p.mcLoc("block/gravel")
			
			p.getVariantBuilder(c.entry).forAllStates { state ->
				val layers = state.getValue(GravelLayerBlock.Layers)
				if(layers == GravelLayerBlock.MaxLayer) {
					ConfiguredModel.builder()
						.modelFile(p.models().getExistingFile(gravel))
						.build()
				} else {
					val modelFile = p.models().getBuilder("block/gravel_height${layers * 2}").apply {
						texture("particle", gravel)
						texture("texture", gravel)
						element().apply {
							val y = (layers * 2).toFloat()
							
							from(0f, 0f, 0f)
							to(16f, y, 16f)
							
							allFaces { direction, builder ->
								builder.texture("#texture")
								if(direction.axis == Direction.Axis.Y) {
									builder.uvs(0f, 0f, 16f, 16f)
								} else {
									builder.uvs(0f, 16f - y, 16f, 16f)
								}
								if(direction != Direction.DOWN) {
									builder.cullface(direction)
								}
							}
						}
					}
					
					ConfiguredModel.builder()
						.modelFile(modelFile)
						.build()
				}
			}
		}
		item()
			.model { c, p -> p.withExistingParent(c.name, p.modLoc("block/gravel_height2")) }
			.build()
	}
	
	
	/* val GravelLayerByte = Registry.block("gravel_layer_byte", ::GravelLayerByteBlock) {
		initialProperties(Blocks::GRAVEL)
		blockstate { c, p ->
			val gravel = p.mcLoc("block/gravel")
			
			p.getVariantBuilder(c.entry).forAllStates { state ->
				val modelFile = p.models().getBuilder(
					"block/gravel_byte" +
						"_" + state.getValue(GravelLayerByteBlock.LayersA) +
						"_" + state.getValue(GravelLayerByteBlock.LayersB) +
						"_" + state.getValue(GravelLayerByteBlock.LayersC) +
						"_" + state.getValue(GravelLayerByteBlock.LayersD)
				).apply {
					texture("particle", gravel)
					texture("texture", gravel)
					
					fun layerElement(x: Float, y: Float, z: Float) {
						if(y == 0f) return
						
						element().apply {
							val delta = 8f
							
							from(x, 0f, z)
							to(x + delta, y, z + delta)
							
							allFaces { direction, builder ->
								builder.texture("#texture")
								if(direction.axis == Direction.Axis.Y) {
									builder.uvs(x, z, x + delta, z + delta)
								} else {
									builder.uvs(x, 16f - y, x + delta, 16f)
								}
								if(direction != Direction.DOWN) {
									builder.cullface(direction)
								}
							}
						}
					}
					
					val multiplier = 16f / GravelLayerByteBlock.MaxLayer
					layerElement(0f, 0f, multiplier * state.getValue(GravelLayerByteBlock.LayersA))
					layerElement(8f, 0f, multiplier * state.getValue(GravelLayerByteBlock.LayersB))
					layerElement(0f, 8f, multiplier * state.getValue(GravelLayerByteBlock.LayersC))
					layerElement(8f, 8f, multiplier * state.getValue(GravelLayerByteBlock.LayersD))
				}
				
				ConfiguredModel.builder()
					.modelFile(modelFile)
					.build()
			}
		}
		item()
			.model { c, p -> p.withExistingParent(c.name, p.modLoc("block/gravel_byte_1_0_0_0")) }
			.build()
	} */
	
	val AdvancedMechanicalRoller = Registry.block("advanced_mechanical_roller", ::AdvancedRollerBlock) {
		initialProperties(SharedProperties::stone)
		properties {
			it.mapColor(MapColor.COLOR_GRAY)
				.noOcclusion()
		}
		
		tag(BlockTags.MINEABLE_WITH_AXE)
		tag(BlockTags.MINEABLE_WITH_PICKAXE)
		
		blockstate { c, p ->
			p.horizontalBlock(c.entry) {
				p.models().getExistingFile(Create.asResource("block/mechanical_roller/block"))
			}
		}
		
		@Suppress("removal", "DEPRECATION")
		addLayer { Supplier(RenderType::cutoutMipped) }
		
		item(::RollerBlockItem)
			.tag(CreateTags.AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.model { c, p -> p.withExistingParent(c.name, Create.asResource("block/mechanical_roller/item")) }
			.build()
		
		onRegister(MovementBehaviour.movementBehaviour(AdvancedRollerMovementBehavior()))
	}
	
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
		blockstate { c, p -> p.simpleBlock(c.get(), p.models().getExistingFile(p.mcLoc("block/air"))) }
		lang("Middle Track Block")
	}
	
	val FlexiTrack = Registry.flexiTrackBlock(
		name = "flexi_track",
		material = TrackMaterial.ANDESITE,
		normalBlock = CreateBlocks.TRACK,
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
	) {
		tag(AllTags.Features.SplitGraph.block)
		lang("Split Graph Train Track")
		addValidToBlockEntity()
	}
	
	val FlexiSplitGraphTrack: BlockEntry<FlexiSplitGraphTrackBlock> = Registry.flexiTrackBlock(
		name = "flexi_split_graph_track",
		material = TrackMaterial.ANDESITE,
		normalBlock = SplitGraphTrack,
		factory = ::FlexiSplitGraphTrackBlock,
	) {
		tag(AllTags.Features.SplitGraph.block)
		lang("Flexible Split Graph Train Track")
	}
}


@Suppress("DEPRECATION", "removal")
inline fun <Track, Material : TrackMaterial> RailXRegistrate.trackBlock(
	name: String,
	material: Material,
	crossinline factory: (BlockBehaviour.Properties, Material) -> Track,
	crossinline blockStates: (DataGenContext<Block, Track>, RegistrateBlockstateProvider) -> Unit,
	createItem: Boolean = true,
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
	tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED)
	tag(CreateTags.AllBlockTags.TRACKS.tag)
	if(!CompatMods.railways || material.trackType != CRTrackMaterials.CRTrackType.MONORAIL) // issue with cinit
		tag(CreateTags.AllBlockTags.GIRDABLE_TRACKS.tag)
	
	if(createItem) item(factory = ::TrackBlockItem) {
		tag(CreateTags.AllItemTags.TRACKS.tag)
		model { c, p -> p.generated(c, Create.asResource("item/track")) }
		
		if(
			CompatMods.railways && (
				material == CRTrackMaterials.PHANTOM ||
					material == CRTrackMaterials.getWide(CRTrackMaterials.PHANTOM) ||
					material == CRTrackMaterials.getNarrow(CRTrackMaterials.PHANTOM)
				)
		) tag(CRTags.AllItemTags.PHANTOM_TRACK_REVEALING.tag)
	}
	
	builder()
}

inline fun <Track : FlexiTrackBlock> RailXRegistrate.flexiTrackBlock(
	name: String,
	material: TrackMaterial,
	normalBlock: DeferredHolder<Block, out TrackBlock>,
	crossinline factory: (BlockBehaviour.Properties, TrackMaterial) -> Track,
	crossinline blockStates: (DataGenContext<Block, Track>, RegistrateBlockstateProvider) -> Unit = { c, p ->
		p.simpleBlock(c.entry, p.models().getExistingFile(Create.asResource("block/track/x_ortho")))
	},
	builder: BlockBuilder<Track, RailXRegistrate>.() -> Unit,
): BlockEntry<Track> = trackBlock(name, material, factory, blockStates, createItem = false) {
	loot { table, block -> table.dropOther(block, block.normalBlock) }
	
	builder()
}.also { entry ->
	FlexiTrackMaterial.addMaterial(original = normalBlock, flexiTrack = entry)
}
