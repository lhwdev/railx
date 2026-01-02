package com.lhwdev.minecraft.railx.registry

import com.tterrag.registrate.builders.BlockBuilder
import net.minecraft.client.renderer.block.BlockModelShaper
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.function.Supplier


private typealias GetModel = BlockModels.Context.() -> BlockModels.ModelMapper


interface ConfigureBlockModel {
	fun model(getModel: GetModel): GetModel
}

@Mod.EventBusSubscriber(value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object BlockModels : ConfigureBlockModel {
	class Context(val modelBakery: ModelBakery) {
		var location: ModelResourceLocation = ModelBakery.MISSING_MODEL_LOCATION
	}
	
	fun interface ModelMapper {
		fun getModel(context: Context, state: BlockState): BakedModel
	}
	
	val entries = mutableMapOf<Block, GetModel>()
	
	
	@SubscribeEvent
	fun onModelBake(event: ModelEvent.ModifyBakingResult) {
		val models = event.models
		val context = Context(modelBakery = event.modelBakery)
		for((block, getModel) in entries) {
			val mapper = getModel(context)
			for(state in block.stateDefinition.possibleStates) {
				val location = BlockModelShaper.stateToModelLocation(state)
				
				context.location = location
				models[location] = mapper.getModel(context, state)
			}
		}
	}
	
	
	override fun model(getModel: GetModel): GetModel = getModel
}


inline fun ConfigureBlockModel.forAllStates(crossinline map: BlockModels.Context.() -> BakedModel): GetModel = model {
	val model = map()
	BlockModels.ModelMapper { _, _ -> model }
}

fun ConfigureBlockModel.copyFromState(from: Supplier<BlockState>): GetModel =
	forAllStates { modelBakery.bakedTopLevelModels[BlockModelShaper.stateToModelLocation(from.get())]!! }

inline fun ConfigureBlockModel.copyFromBlock(
	from: Supplier<out Block>,
	crossinline configure: (BlockState) -> BlockState,
): GetModel = copyFromState(from = { from.get().defaultBlockState().let(configure) })


fun <T : Block, P> BlockBuilder<T, P>.blockModel(getModel: GetModel) {
	onRegister { BlockModels.entries[it] = getModel }
	blockstate { _, _ -> }
}

val <T : Block, P> BlockBuilder<T, P>.blockModel: ConfigureBlockModel
	get() = object : ConfigureBlockModel {
		override fun model(getModel: GetModel): GetModel = getModel
			.also { blockModel(getModel) }
	}
