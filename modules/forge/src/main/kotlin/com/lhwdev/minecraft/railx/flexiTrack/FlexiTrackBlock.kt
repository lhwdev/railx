package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.mixin.flexiTrack.BlockAccessor
import com.lhwdev.minecraft.railx.mixin.flexiTrack.TrackBlockEntityAccessor
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
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.transform.Affine
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.multiplayer.ClientLevel
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
import kotlin.math.atan2
import kotlin.math.min
import com.simibubi.create.AllBlocks as CreateBlocks


class FlexiTrackBlock(
	properties: Properties,
	@get:JvmName("getMaterialKt")
	val material: TrackMaterial,
) : Block(properties),
	IBE<FlexiTrackBlockEntity>,
	IWrenchable,
	ITrackBlock,
	SpecialBlockItemRequirement,
	ProperWaterloggedBlock,
	IHaveBigOutline {
	companion object {
		// val BaseDirection = FlexiDirectionProperty.create("direction")
		val Waterlogged: BooleanProperty = TrackBlock.WATERLOGGED
		
		fun blockEntity(world: BlockGetter, pos: BlockPos): FlexiTrackBlockEntity? =
			world.getBlockEntity(pos) as? FlexiTrackBlockEntity
		
		fun flexiShape(world: BlockGetter, pos: BlockPos): FlexiShape =
			(world.getBlockEntity(pos) as? FlexiTrackBlockEntity)?.shape ?: FlexiShape.Empty
	}
	
	init {
		val stateDefinition = StateDefinition.Builder<Block, BlockState>(this).let { builder ->
			createBlockStateDefinition(builder)
			builder.create(Block::defaultBlockState) { block, values, propertiesCodec ->
				FlexiBlockState(block, values, propertiesCodec, FlexiShape.Empty, null)
			}
		}
		@Suppress("CAST_NEVER_SUCCEEDS")
		(this as BlockAccessor).setStateDefinition(stateDefinition)
		
		registerDefaultState(
			stateDefinition.possibleStates[0]
				// .setValue(BaseDirection, FlexiDirection.Known.Divisions[0])
				.setValue(Waterlogged, false)
		)
	}
	
	private val BlockState.flexi: FlexiBlockState
		get() = this as FlexiBlockState
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder.add(/* BaseDirection,  */Waterlogged))
	}
	
	
	override fun getBlockPathType(state: BlockState, level: BlockGetter, pos: BlockPos, mob: Mob?): PathType =
		PathType.RAIL
	
	override fun getFluidState(state: BlockState): FluidState =
		fluidState(state)
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		val stateForPlacement = withWater(super.getStateForPlacement(context), context)
			as FlexiBlockState
		val player = context.player
		if(player == null) return stateForPlacement
		
		var lookAngle = player.lookAngle.multiply(1.0, 0.0, 1.0)
		if(Mth.equal(lookAngle.length(), 0.0)) {
			lookAngle = VecHelper.rotate(Vec3(0.0, 0.0, 1.0), -player.yRot.toDouble(), Direction.Axis.Y)
		}
		
		return stateForPlacement.setShape(
			FlexiShape.Single(
				FlexiDirection.Known.roundFromAngle(atan2(lookAngle.z, lookAngle.x))
			)
		)
	}
	
	override fun getPistonPushReaction(pState: BlockState): PushReaction =
		PushReaction.BLOCK
	
	override fun playerWillDestroy(pLevel: Level, pPos: BlockPos, pState: BlockState, pPlayer: Player): BlockState {
		super.playerWillDestroy(pLevel, pPos, pState, pPlayer)
		
		if(pLevel.isClientSide) return pState
		if(!pPlayer.isCreative) return pState
		withBlockEntityDo(pLevel, pPos) { be ->
			@Suppress("CAST_NEVER_SUCCEEDS")
			(be as TrackBlockEntityAccessor).cancelDrops = true
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
	
	override fun getYOffsetAt(world: BlockGetter, pos: BlockPos, state: BlockState?, end: Vec3?): Int =
		getBlockEntity(world, pos)?.tilt?.getYOffsetForAxisEnd(end) ?: 0
	
	override fun getConnected(
		worldIn: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		linear: Boolean,
		connectedTo: TrackNodeLocation?,
	): Collection<DiscoveredLocation> {
		val list: Collection<DiscoveredLocation>
		val world = if(connectedTo != null && worldIn is ServerLevel) {
			worldIn.server.getLevel(connectedTo.dimension)!!
		} else {
			worldIn
		}
		
		val blockEntity = world.getBlockEntity(pos)
		if(blockEntity !is FlexiTrackBlockEntity) return emptyList()
		
		val shape = blockEntity.shape
		if(shape.axes.size > 1) {
			list = mutableListOf()
			val center = Vec3.atBottomCenterOf(pos)
				.add(0.0, getElevationAtCenter(world, pos, state), 0.0)
			
			for(axis in shape.axes) {
				for(fromCenter in Iterate.trueAndFalse) ITrackBlock.addToListIfConnected(
					connectedTo,
					list,
					{ d, b ->
						axis.tangent.scale((if(b) 0.0 else if(fromCenter) -d else d))
							.add(center)
					},
					{ b -> shape.normal },
					{ b -> if(world is Level) world.dimension() else Level.OVERWORLD },
					{ v -> 0 },
					axis.tangent,
					null,
					{ b, v -> ITrackBlock.getMaterialSimple(world, v) })
			}
		} else list = super.getConnected(world, pos, state, linear, connectedTo)
		
		if(linear) return list
		
		val connections = blockEntity.connections
		connections.forEach { (_, bc) ->
			ITrackBlock.addToListIfConnected(
				connectedTo,
				list,
				{ d, b ->
					if(d == 1.0) Vec3.atLowerCornerOf(bc.bePositions.get(b)) else bc.starts.get(b)
				},
				{ first -> bc.normals.get(first) },
				{ b -> if(world is Level) world.dimension() else Level.OVERWORLD },
				{ end -> bc.yOffsetAt(end) },
				null,
				bc,
				{ b, v -> ITrackBlock.getMaterialSimple(world, v, bc.material) })
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
		var removeBE = false
		if(!pState.`is`(pNewState.block)) {
			val blockEntity = pLevel.getBlockEntity(pPos)
			if(blockEntity is FlexiTrackBlockEntity && !pLevel.isClientSide) {
				blockEntity.removeInboundConnections(true)
			}
			removeBE = true
		}
		
		if(pNewState.block !== this || pState !== pNewState) {
			TrackPropagator.onRailRemoved(pLevel, pPos, pState)
		}
		if(removeBE) {
			pLevel.removeBlockEntity(pPos)
		}
		if(!pLevel.isClientSide) {
			updateGirders(pState, pLevel, pPos, pLevel.blockTicks)
		}
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
		state.flexi.shape.normal
	
	override fun getTrackAxes(world: BlockGetter, pos: BlockPos, state: BlockState): List<Vec3> =
		state.flexi.shape.tangents
	
	override fun getCurveStart(world: BlockGetter, pos: BlockPos, state: BlockState, axis: Vec3): Vec3 {
		val vertical = axis.y != 0.0
		return VecHelper.getCenterOf(pos)
			.add(0.0, (if(vertical) 0f else -.5f).toDouble(), 0.0)
			.add(axis.scale(.5))
	}
	
	override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult =
		InteractionResult.SUCCESS
	
	override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
		val player = context.player!!
		val level = context.level
		if(!level.isClientSide && !player.isCreative) {
			val blockEntity = level.getBlockEntity(context.clickedPos)
			if(blockEntity is FlexiTrackBlockEntity) {
				@Suppress("CAST_NEVER_SUCCEEDS")
				(blockEntity as TrackBlockEntityAccessor).cancelDrops = true
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
		var diff: Vec3? = null
		var normal: Vec3? = null
		val offset: Vec3?
		
		val be = world.getBlockEntity(pos)
		if(be !is FlexiTrackBlockEntity) return null
		if(bezierPoint != null) {
			val bc = be.connections[bezierPoint.curveTarget()]
			if(bc != null) {
				val length = Mth.floor(bc.getLength() * 2).toDouble()
				val seg = bezierPoint.segment() + 1
				val t = seg / length
				val tpre = (seg - 1) / length
				val tpost = (seg + 1) / length
				
				offset = bc.getPosition(t)
				normal = bc.getNormal(t)
				diff = bc.getPosition(tpost)
					.subtract(bc.getPosition(tpre))
					.normalize()
				
				affine.translate(offset.subtract(Vec3.atBottomCenterOf(pos)))
				affine.translate(0f, -4 / 16f, 0f)
			} else return null
		}
		
		if(normal == null) {
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
				if(axis.y != 0.0) direction.step * 2.5f / 16f else 0f
			)
		} else {
			affine.translate(0f, 4 / 16f, 0f)
			if(direction == Direction.AxisDirection.NEGATIVE) {
				affine.rotateCentered(Mth.PI, Direction.UP)
			}
		}
		
		if(bezierPoint == null && be.isTilted) {
			var yOffset = 0.0
			for(bc in be.connections.values) yOffset += bc.starts.getFirst().y - pos.y
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
	
	override fun getMaterial(): TrackMaterial = material
	
	class RenderProperties : ReducedDestroyEffects(), MultiPosDestructionHandler {
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
