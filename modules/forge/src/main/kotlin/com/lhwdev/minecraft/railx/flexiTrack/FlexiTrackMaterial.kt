package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.registry.AllBlocks
import com.simibubi.create.Create
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackMaterial.TrackModelHolder
import com.simibubi.create.content.trains.track.TrackMaterial.TrackType
import com.simibubi.create.content.trains.track.TrackMaterial.TrackType.TrackBlockFactory
import com.simibubi.create.foundation.data.recipe.CommonMetal
import com.tterrag.registrate.util.nullness.NonNullSupplier
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.common.Tags
import thedarkcolour.kotlinforforge.neoforge.forge.runWhenOn
import java.util.function.Supplier
import java.util.stream.Stream
import com.simibubi.create.AllPartialModels as CreatePartialModels


val TrackMaterial.realBlock: ITrackBlock
	get() = when(this) {
		is FlexiTrackMaterial -> flexiBlock
		else -> block
	}

fun TrackMaterial.defaultBlockState(): BlockState =
	(realBlock as Block).defaultBlockState()


class FlexiTrackMaterial(
	id: ResourceLocation,
	langName: String,
	val flexiBlockSupplier: NonNullSupplier<out FlexiTrackBlock>,
	particle: ResourceLocation,
	sleeperIngredient: Ingredient,
	railsIngredient: Ingredient,
	trackType: TrackType,
	modelHolder: Supplier<Supplier<TrackModelHolder?>>,
	customFactory: TrackBlockFactory? = null,
) : TrackMaterial(
	id,
	langName,
	{ NonNullSupplier { flexiBlockSupplier.get().normalBlock } },
	particle,
	sleeperIngredient,
	railsIngredient,
	trackType,
	modelHolder,
	customFactory
) {
	val flexiBlock: FlexiTrackBlock
		get() = flexiBlockSupplier.get()
	
	override fun asStack(count: Int): ItemStack =
		ItemStack(flexiBlock, count)
	
	
	companion object {
		val Andesite: FlexiTrackMaterial = FlexiTrackMaterial(RailX.asResource("flexi_andesite")) {
			langName = "Andesite"
			trackBlock = AllBlocks.FlexiTrack
			particle = Create.asResource("block/palettes/stone_types/polished/andesite_cut_polished")
			defaultModels()
		}
		
		init {
			// legacy material migration
			ALL[RailX.asResource("andesite")] = Andesite
		}
		
		fun allFlexiBlocks(): List<NonNullSupplier<out FlexiTrackBlock>> = ALL.values
			.filterIsInstance<FlexiTrackMaterial>()
			.map { it.flexiBlockSupplier }
	}
}


inline fun FlexiTrackMaterial(id: ResourceLocation, block: FlexiTrackMaterialFactory.() -> Unit): FlexiTrackMaterial =
	FlexiTrackMaterialFactory(id).apply(block).build()

class FlexiTrackMaterialFactory(private val id: ResourceLocation) {
	var langName: String? = null
	
	var trackBlock: NonNullSupplier<out FlexiTrackBlock>? = null
	
	var sleeperIngredient: Ingredient = Ingredient.EMPTY
	
	var railsIngredient: Ingredient = Ingredient.fromValues(
		Stream.of(
			Ingredient.TagValue(Tags.Items.NUGGETS_IRON),
			Ingredient.TagValue(CommonMetal.ZINC.nuggets),
		)
	)
	
	var particle: ResourceLocation? = null
	
	var trackType: TrackType = TrackType.STANDARD
	
	var customFactory: TrackBlockFactory? = null
	
	@field:OnlyIn(Dist.CLIENT)
	var modelHolder: TrackModelHolder? = null
		@OnlyIn(Dist.CLIENT) get
		@OnlyIn(Dist.CLIENT) set
	
	@OnlyIn(Dist.CLIENT)
	var tieModel: PartialModel? = null
		@OnlyIn(Dist.CLIENT) get
		@OnlyIn(Dist.CLIENT) set
	
	@OnlyIn(Dist.CLIENT)
	var leftSegmentModel: PartialModel? = null
		@OnlyIn(Dist.CLIENT) get
		@OnlyIn(Dist.CLIENT) set
	
	@OnlyIn(Dist.CLIENT)
	var rightSegmentModel: PartialModel? = null
		@OnlyIn(Dist.CLIENT) get
		@OnlyIn(Dist.CLIENT) set
	
	fun defaultModels() {
		runWhenOn(Dist.CLIENT) {
			modelHolder = TrackModelHolder(
				CreatePartialModels.TRACK_TIE,
				CreatePartialModels.TRACK_SEGMENT_LEFT,
				CreatePartialModels.TRACK_SEGMENT_RIGHT
			)
		}
	}
	
	fun noRecipeGen() {
		this.railsIngredient = Ingredient.EMPTY
		this.sleeperIngredient = Ingredient.EMPTY
	}
	
	fun standardModels() {
		runWhenOn(Dist.CLIENT) {
			val namespace = id.namespace
			val prefix = "block/track/${id.path}/"
			tieModel = PartialModel.of(ResourceLocation.fromNamespaceAndPath(namespace, prefix + "tie"))
			leftSegmentModel =
				PartialModel.of(ResourceLocation.fromNamespaceAndPath(namespace, prefix + "segment_left"))
			rightSegmentModel =
				PartialModel.of(ResourceLocation.fromNamespaceAndPath(namespace, prefix + "segment_right"))
		}
	}
	
	fun customModels(
		tieModel: () -> PartialModel,
		leftSegmentModel: () -> PartialModel,
		rightSegmentModel: () -> PartialModel,
	) {
		runWhenOn(Dist.CLIENT) {
			this.tieModel = tieModel()
			this.leftSegmentModel = leftSegmentModel()
			this.rightSegmentModel = rightSegmentModel()
		}
	}
	
	fun build(): FlexiTrackMaterial {
		runWhenOn(Dist.CLIENT) {
			checkNotNull(modelHolder)
			if(tieModel != null || leftSegmentModel != null || rightSegmentModel != null) {
				assert(tieModel != null && leftSegmentModel != null && rightSegmentModel != null)
				modelHolder = TrackModelHolder(tieModel, leftSegmentModel, rightSegmentModel)
			}
		}
		return FlexiTrackMaterial(
			id,
			langName!!,
			trackBlock!!,
			particle!!,
			sleeperIngredient,
			railsIngredient,
			trackType,
			{ Supplier { modelHolder } },
			customFactory,
		)
	}
}
