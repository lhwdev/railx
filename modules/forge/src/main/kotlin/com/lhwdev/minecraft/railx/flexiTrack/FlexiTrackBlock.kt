package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.common.addIfConnected
import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement
import com.simibubi.create.content.decoration.girder.GirderBlock
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.IHaveBigOutline
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.transform.Affine
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap
import net.createmod.catnip.data.Iterate
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.client.particle.TerrainParticle
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.ticks.LevelTickAccess
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import kotlin.math.max
import kotlin.math.min
import com.simibubi.create.AllBlocks as CreateBlocks


// private val Properties_offsetFunction = Properties::class.java.getDeclaredField("offsetFunction")
// 	.also { it.isAccessible = true }
//
// private fun Properties.offsetFunction(fn: BlockBehaviour.OffsetFunction): Properties {
// 	Properties_offsetFunction.set(this, fn)
// 	return this
// }


open class FlexiTrackBlock(
	properties: Properties,
	@get:JvmName("getMaterialKt")
	val material: FlexiTrackMaterial,
) : Block(
	properties
		.dynamicShape()
),
	IBE<FlexiTrackBlockEntity>,
	IWrenchable,
	ITrackBlock,
	SpecialBlockItemRequirement,
	ProperWaterloggedBlock,
	IHaveBigOutline {
	companion object {
		// val BaseDirection = FlexiDirectionProperty.create("direction")
		val Waterlogged: BooleanProperty = ProperWaterloggedBlock.WATERLOGGED
	}
	
	init {
		val stateDefinition = StateDefinition.Builder<Block, BlockState>(this).let { builder ->
			createBlockStateDefinition(builder)
			builder.create(Block::defaultBlockState) { block, values, propertiesCodec ->
				FlexiBlockState.create(block as FlexiTrackBlock, values, propertiesCodec)
			}
		}
		this.stateDefinition = stateDefinition
		
		registerDefaultState(
			stateDefinition.possibleStates[0]
				// .setValue(BaseDirection, FlexiDirection.Known.Divisions[0])
				.setValue(Waterlogged, false)
		)
	}
	
	
	fun blockEntity(world: BlockGetter, pos: BlockPos): FlexiTrackBlockEntity? =
		world.getBlockEntity(pos) as? FlexiTrackBlockEntity
	
	fun flexiState(world: BlockGetter, pos: BlockPos): FlexiState =
		blockEntity(world, pos)?.state ?: FlexiState.Base
	
	fun flexiShape(world: BlockGetter, pos: BlockPos): FlexiShape =
		flexiState(world, pos).shape
	
	
	private val BlockState.flexi: FlexiBlockState
		get() = this as FlexiBlockState
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder.add(/* BaseDirection,  */Waterlogged))
	}
	
	
	val normalBlock: TrackBlock
		get() = CreateBlocks.TRACK.get()
	
	override fun getRenderShape(state: BlockState): RenderShape =
		RenderShape.INVISIBLE
	
	override fun getBlockPathType(state: BlockState, level: BlockGetter, pos: BlockPos, mob: Mob?): PathType =
		PathType.RAIL
	
	override fun getFluidState(state: BlockState): FluidState =
		fluidState(state)
	
	/** Note that track rotation is taken care by FlexiTrackBlockItem. */
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
		withWater(super.getStateForPlacement(context), context)
	
	override fun getPistonPushReaction(pState: BlockState): PushReaction =
		PushReaction.BLOCK
	
	override fun playerWillDestroy(pLevel: Level, pPos: BlockPos, pState: BlockState, pPlayer: Player): BlockState {
		super.playerWillDestroy(pLevel, pPos, pState, pPlayer)
		
		if(pLevel.isClientSide) return pState
		if(!pPlayer.isCreative) return pState
		withBlockEntityDo(pLevel, pPos) { be ->
			(be as FlexiTrackBlockEntity).willCancelDrop = true
			be.removeInboundConnections(true)
		}
		
		return pState
	}
	
	public override fun onPlace(
		pState: BlockState,
		pLevel: Level,
		pPos: BlockPos,
		pOldState: BlockState,
		pIsMoving: Boolean,
	) {
		if(pState === pOldState) return
		if(pLevel.isClientSide) return
		val blockTicks = pLevel.blockTicks
		if(!blockTicks.hasScheduledTick(pPos, this)) pLevel.scheduleTick(pPos, this, 1)
		updateGirders(pState, pLevel, pPos, blockTicks)
	}
	
	override fun setPlacedBy(
		pLevel: Level,
		pPos: BlockPos,
		pState: BlockState,
		pPlacer: LivingEntity?,
		pStack: ItemStack,
	) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack)
		withBlockEntityDo(pLevel, pPos) { it.validateConnections() }
	}
	
	public override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, randomSource: RandomSource) {
		TrackPropagator.onRailAdded(level, pos, state)
		withBlockEntityDo(level, pos) { it.tilt.undoSmoothing() }
	}
	
	public override fun updateShape(
		state: BlockState, pDirection: Direction, pNeighborState: BlockState,
		level: LevelAccessor, pCurrentPos: BlockPos, pNeighborPos: BlockPos,
	): BlockState {
		updateWater(level, state, pCurrentPos)
		return state
	}
	
	
	class NearestAxis(val axis: FlexiDirection, val sign: Direction.AxisDirection) {
		val signedAxis: FlexiDirection get() = if(sign == Direction.AxisDirection.POSITIVE) axis else -axis
	}
	
	fun getNearestTrackDirection(
		world: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		lookVec: Vec3,
	): NearestAxis? {
		var best: FlexiDirection? = null
		var bestDiff = Double.MAX_VALUE
		for(axis in flexiShape(world, pos).axes) {
			for(opposite in Iterate.positiveAndNegative) {
				val distanceTo = axis.tangent.distanceTo(lookVec.scale(opposite.toDouble()))
				if(distanceTo > bestDiff) continue
				bestDiff = distanceTo
				best = axis
			}
		}
		if(best == null) return null
		val direction = if(lookVec.dot(best.tangent.multiply(1.0, 0.0, 1.0)) >= 0) {
			Direction.AxisDirection.POSITIVE
		} else {
			Direction.AxisDirection.NEGATIVE
		}
		return NearestAxis(best, direction)
	}
	
	
	override fun getYOffsetAt(world: BlockGetter, pos: BlockPos, state: BlockState?, end: Vec3?): Int = 0
	
	override fun getElevationAtCenter(world: BlockGetter, pos: BlockPos, state: BlockState): Double = 0.0
	
	override fun getConnected(
		worldIn: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		linear: Boolean,
		connectedTo: TrackNodeLocation?,
	): Collection<DiscoveredLocation> {
		val world = if(connectedTo != null && worldIn is ServerLevel) {
			worldIn.server.getLevel(connectedTo.dimension)!!
		} else {
			worldIn
		}
		
		val blockEntity = world.getBlockEntity(pos)
		if(blockEntity !is FlexiTrackBlockEntity) return emptyList()
		
		val shape = blockEntity.shape
		val center = getTrackBase(world, pos, state)
		
		val list = mutableListOf<DiscoveredLocation>()
		for(axis in shape.axes) {
			for(direction in Direction.AxisDirection.entries) list.addIfConnected(
				fromEnd = connectedTo,
				getOffset = { t, first -> center + axis.tangent.scale(if(first) 0.0 else direction.step * t) },
			) { DiscoveredLocation(level = world, normal = shape.normal, tangent = axis.tangent) }
		}
		
		if(linear) return list
		
		val connections = blockEntity.connections
		for((_, bc) in connections) list.addIfConnected(
			fromEnd = connectedTo,
			getOffset = { t, first ->
				if(t == 1.0) Vec3.atLowerCornerOf(bc.bePositions.get(first))
				else bc.starts.get(first)
			}
		) {
			DiscoveredLocation(
				level = world,
				normal = bc.normals.get(isFirst),
				tangent = null,
				yOffset = bc.yOffsetAt(offsetCenter),
				viaTurn = bc,
			)
		}
		
		return list
	}
	
	public override fun onRemove(
		pState: BlockState,
		pLevel: Level,
		pPos: BlockPos,
		pNewState: BlockState,
		pIsMoving: Boolean,
	) {
		if(pState.block != pNewState.block) {
			val blockEntity = pLevel.getBlockEntity(pPos)
			if(blockEntity is FlexiTrackBlockEntity && !pLevel.isClientSide) {
				blockEntity.removeInboundConnections(true)
			}
		}
		
		if(pNewState.block != this || pState != pNewState)
			TrackPropagator.onRailRemoved(pLevel, pPos, pState)
		if(!pLevel.isClientSide)
			updateGirders(pState, pLevel, pPos, pLevel.blockTicks)
		
		super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
	}
	
	// No assembly on flexi track
	//    override fun useItemOn(
	//        stack: ItemStack,
	//        state: BlockState,
	//        level: Level,
	//        pos: BlockPos,
	//        player: Player,
	//        hand: InteractionHand,
	//        hitResult: BlockHitResult
	//    ): ItemInteractionResult {
	//        if (level.isClientSide) return ItemInteractionResult.SUCCESS
	//        for (entry in StationBlockEntity.assemblyAreas.get(level).entries) {
	//            if (!entry.value.isInside(pos)) continue
	//            val station = level.getBlockEntity(entry.key)
	//            if (station is StationBlockEntity && station.trackClicked(player, hand, this, state, pos))
	//                return ItemInteractionResult.SUCCESS
	//        }
	//
	//        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
	//    }
	
	private fun updateGirders(pState: BlockState, pLevel: Level, pPos: BlockPos, blockTicks: LevelTickAccess<Block?>) {
		for(axis in getTrackAxes(pLevel, pPos, pState)) {
			if(axis.length() > 1 || axis.y != 0.0) continue
			for(side in Iterate.positiveAndNegative) {
				val girderPos = pPos.below()
					.offset(BlockPos.containing(axis.z * side, 0.0, axis.x * side))
				val girderState = pLevel.getBlockState(girderPos)
				val block = girderState.block
				if(block is GirderBlock && !blockTicks.hasScheduledTick(girderPos, block)) {
					pLevel.scheduleTick(girderPos, block, 1)
				}
			}
		}
	}
	
	override fun canSurvive(state: BlockState, reader: LevelReader, pos: BlockPos): Boolean =
		reader.getBlockState(pos.below()).block !== this
	
	override fun getShape(
		state: BlockState,
		world: BlockGetter,
		pos: BlockPos,
		collisionContext: CollisionContext,
	): VoxelShape = blockEntity(world, pos)?.voxelShape() ?: Shapes.empty()
	
	override fun getInteractionShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape =
		blockEntity(world, pos)?.voxelShape() ?: Shapes.empty()
	
	override fun getCollisionShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext,
	): VoxelShape = Shapes.empty()
	
	override fun newBlockEntity(pos: BlockPos, state: BlockState): FlexiTrackBlockEntity =
		AllBlockEntityTypes.FlexiTrack.create(pos, state)
	
	override fun getBlockEntityType(): BlockEntityType<out FlexiTrackBlockEntity> =
		AllBlockEntityTypes.FlexiTrack.get()
	
	override fun getBlockEntityClass(): Class<FlexiTrackBlockEntity> =
		FlexiTrackBlockEntity::class.java
	
	override fun getUpNormal(world: BlockGetter, pos: BlockPos, state: BlockState): Vec3 =
		flexiShape(world, pos).normal
	
	override fun getTrackAxes(world: BlockGetter, pos: BlockPos, state: BlockState): List<Vec3> =
		flexiShape(world, pos).tangents
	
	fun getTrackBase(world: BlockGetter, pos: BlockPos, state: BlockState): Vec3 =
		Vec3.atBottomCenterOf(pos).add(0.0, getElevationAtCenter(world, pos, state), 0.0)
	
	override fun getCurveStart(world: BlockGetter, pos: BlockPos, state: BlockState, axis: Vec3): Vec3 =
		getTrackBase(world, pos, state) + axis.scale(.5)
	
	override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult =
		InteractionResult.SUCCESS
	
	override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
		val player = context.player!!
		val level = context.level
		if(!level.isClientSide && !player.isCreative) {
			val blockEntity = level.getBlockEntity(context.clickedPos)
			if(blockEntity is FlexiTrackBlockEntity) {
				blockEntity.willCancelDrop = true
				blockEntity.connections.values.forEach { it.addItemsToPlayer(player) }
			}
		}
		
		return super.onSneakWrenched(state, context)
	}
	
	override fun overlay(world: BlockGetter, pos: BlockPos, existing: BlockState, placed: BlockState): BlockState =
		existing
	
	override fun rotate(state: BlockState, rotation: Rotation): BlockState =
		state.flexi.mapShape { it.rotate(rotation) }
	
	override fun mirror(state: BlockState, mirror: Mirror): BlockState =
		state.flexi.mapShape { it.mirror(mirror) }
	
	override fun getBogeyAnchor(world: BlockGetter, pos: BlockPos, state: BlockState): BlockState =
		CreateBlocks.SMALL_BOGEY.defaultState
	
	@OnlyIn(Dist.CLIENT)
	override fun prepareAssemblyOverlay(
		world: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		direction: Direction,
		ms: PoseStack,
	): PartialModel? = null
	
	@OnlyIn(Dist.CLIENT)
	override fun <Self : Affine<Self>> prepareTrackOverlay(
		affine: Affine<Self>,
		world: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		bezierPoint: BezierTrackPointLocation?,
		direction: Direction.AxisDirection,
		type: TrackTargetingBehaviour.RenderedTrackOverlayType,
	): PartialModel? {
		var axis: Vec3? = null
		val diff: Vec3
		val normal: Vec3
		
		val be = world.getBlockEntity(pos)
		if(be !is FlexiTrackBlockEntity) return null
		if(bezierPoint != null) {
			val bc = be.connections[bezierPoint.curveTarget] ?: return null
			val t = bc.getSegmentT(bezierPoint.segment + 1).toDouble()
			val tPre = bc.getSegmentT(bezierPoint.segment).toDouble()
			val tPost = bc.getSegmentT((bezierPoint.segment + 2).coerceAtMost(bc.segmentCount)).toDouble()
			
			val offset = bc.getPosition(t)
			normal = bc.getNormal(t)
			diff = bc.getPosition(tPost)
				.subtract(bc.getPosition(tPre))
				.normalize()
			
			affine.translateBack(pos.bottomCenter)
			affine.translate(offset)
			affine.translate(0f, -4 / 16f, 0f)
		} else {
			axis = be.shape.axis1.tangent
			diff = axis.scale(direction.step.toDouble()).normalize()
			normal = getUpNormal(world, pos, state)
		}
		
		val angles = TrackRenderer.getModelAngles(normal, diff)
		
		affine.center()
			.rotateY(angles.y.toFloat())
			.rotateX(angles.x.toFloat())
			.uncenter()
		
		if(axis != null) {
			affine.translate(
				0f,
				if(axis.y != 0.0) 7 / 16f else 0f,
				if(axis.y != 0.0) direction.step * 2.5f / 16f else 0f,
			)
		} else {
			affine.translate(0f, 4 / 16f, 0f)
			if(direction == Direction.AxisDirection.NEGATIVE) {
				affine.rotateCentered(Mth.PI, Direction.UP)
			}
		}
		
		if(bezierPoint == null && be.isTilted) {
			var yOffset = 0.0
			for(bc in be.connections.values) yOffset += bc.starts.first.y - pos.y
			affine.center()
				.rotateXDegrees((-direction.step * be.tilt.smoothingAngle.get()).toFloat())
				.uncenter()
				.translate(0.0, yOffset / 2, 0.0)
		}
		
		return when(type) {
			TrackTargetingBehaviour.RenderedTrackOverlayType.DUAL_SIGNAL -> AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER -> AllPartialModels.TRACK_OBSERVER_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.SIGNAL -> AllPartialModels.TRACK_SIGNAL_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.STATION -> AllPartialModels.TRACK_STATION_OVERLAY
		}
	}
	
	override fun trackEquals(state1: BlockState, state2: BlockState): Boolean =
		state1 == state2
	
	override fun getRequiredItems(state: BlockState, be: BlockEntity?): ItemRequirement {
		var sameTypeTrackAmount = 1
		val otherTrackAmounts = Object2IntArrayMap<TrackMaterial>()
		var girderAmount = 0
		
		if(be is TrackBlockEntity) {
			for(bezierConnection in be.connections.values) {
				if(!bezierConnection.isPrimary) continue
				val material = bezierConnection.material
				if(material == this.material) {
					sameTypeTrackAmount += bezierConnection.trackItemCost
				} else {
					otherTrackAmounts.put(material, otherTrackAmounts.getOrDefault(material, 0) + 1)
				}
				girderAmount += bezierConnection.girderItemCost
			}
		}
		
		val stacks = mutableListOf<ItemStack>()
		while(sameTypeTrackAmount > 0) {
			stacks += ItemStack(state.block, min(sameTypeTrackAmount, 64))
			sameTypeTrackAmount -= 64
		}
		for(material in otherTrackAmounts.keys) {
			var amt = otherTrackAmounts.getOrDefault(material, 0)
			while(amt > 0) {
				stacks += material.asStack(min(amt, 64))
				amt -= 64
			}
		}
		while(girderAmount > 0) {
			stacks += CreateBlocks.METAL_GIRDER.asStack(min(girderAmount, 64))
			girderAmount -= 64
		}
		
		return ItemRequirement(ItemUseType.CONSUME, stacks)
	}
	
	override fun getMaterial(): FlexiTrackMaterial = material
	
	class RenderProperties : IClientBlockExtensions, MultiPosDestructionHandler {
		override fun addDestroyEffects(
			state: BlockState,
			worldIn: Level,
			pos: BlockPos,
			manager: ParticleEngine,
		): Boolean {
			if(worldIn !is ClientLevel) return true
			val shape = state.getShape(worldIn, pos)
			var amtBoxes = 0
			shape.forAllBoxes { _, _, _, _, _, _ -> amtBoxes++ }
			val chance = 1.0 / amtBoxes
			
			if(state.isAir) return true
			
			// TODO: rotate shape
			val particleState = (state.block as FlexiTrackBlock).normalBlock.defaultBlockState()
			shape.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
				val w = x2 - x1
				val h = y2 - y1
				val l = z2 - z1
				val xParts = max(2, Mth.ceil(min(1.0, w) * 4))
				val yParts = max(2, Mth.ceil(min(1.0, h) * 4))
				val zParts = max(2, Mth.ceil(min(1.0, l) * 4))
				for(xIndex in 0..<xParts) {
					for(yIndex in 0..<yParts) {
						for(zIndex in 0..<zParts) {
							if(worldIn.random.nextDouble() > chance) continue
							
							val d4 = (xIndex + .5) / xParts
							val d5 = (yIndex + .5) / yParts
							val d6 = (zIndex + .5) / zParts
							val x = pos.x + d4 * w + x1
							val y = pos.y + d5 * h + y1
							val z = pos.z + d6 * l + z1
							
							manager.add(
								TerrainParticle(worldIn, x, y, z, d4 - 0.5, d5 - 0.5, d6 - 0.5, particleState, pos)
									.updateSprite(particleState, pos)
							)
						}
					}
				}
			}
			return true
		}
		
		override fun getExtraPositions(
			level: ClientLevel,
			pos: BlockPos,
			blockState: BlockState,
			progress: Int,
		): Set<BlockPos>? {
			val blockEntity = level.getBlockEntity(pos)
			return if(blockEntity is FlexiTrackBlockEntity) {
				blockEntity.connections.keys.toSet()
			} else {
				null
			}
		}
	}
}
