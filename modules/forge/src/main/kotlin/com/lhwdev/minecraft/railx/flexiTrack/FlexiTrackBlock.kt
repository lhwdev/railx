package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.common.addIfConnected
import com.lhwdev.minecraft.railx.common.from
import com.lhwdev.minecraft.railx.common.to
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.lhwdev.minecraft.utils.vectors.plus
import com.mojang.blaze3d.vertex.PoseStack
import com.railwayteam.railways.content.custom_tracks.phantom.PhantomSpriteManager
import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing
import com.railwayteam.railways.registry.CRBlockPartials
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.AllPartialModels
import com.simibubi.create.Create
import com.simibubi.create.content.decoration.girder.GirderBlock
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.track.*
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.transform.Affine
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.ticks.LevelTickAccess
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.level.BlockEvent
import java.util.*
import java.util.function.Supplier
import com.simibubi.create.AllSoundEvents as CreateSoundEvents


open class FlexiTrackBlock(properties: Properties, material: TrackMaterial) :
	TrackBlock(properties.dynamicShape(), material) {
	companion object {
		val Waterlogged: BooleanProperty = WATERLOGGED
	}
	
	private var initializingFlexiState = false
	
	init {
		initializingFlexiState = true
		val stateDefinition = StateDefinition.Builder<Block, BlockState>(this).let { builder ->
			createBlockStateDefinition(builder)
			builder.create(Block::defaultBlockState) { block, values, propertiesCodec ->
				FlexiBlockState.create(block as FlexiTrackBlock, values, propertiesCodec)
			}
		}
		this.stateDefinition = stateDefinition
		
		registerDefaultState(
			stateDefinition.possibleStates[0]
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
		if(!initializingFlexiState) return super.createBlockStateDefinition(builder)
		builder.add(Waterlogged)
	}
	
	
	private var _normalBlockSupplier: Supplier<out TrackBlock>? = null
	
	val normalBlockSupplier: Supplier<out TrackBlock>
		get() = _normalBlockSupplier ?: @Suppress("DEPRECATION")
		FlexiTrackMaterial.ToNormal[builtInRegistryHolder().unwrapKey().get()
			.location()]!!.also { _normalBlockSupplier = it }
	
	val normalBlock: TrackBlock
		get() = normalBlockSupplier.get()
	
	override fun getRenderShape(state: BlockState): RenderShape =
		RenderShape.INVISIBLE
	
	/** Note that track rotation is taken care by FlexiTrackBlockItem. */
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
		withWater(defaultBlockState(), context)
	
	override fun getCloneItemStack(
		state: BlockState,
		target: HitResult,
		level: BlockGetter,
		pos: BlockPos,
		player: Player,
	): ItemStack = normalBlock.getCloneItemStack(state, target, level, pos, player)
	
	override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player) {
		if(!level.isClientSide && player.isCreative)
			blockEntity(level, pos)?.let { it.willCancelDrop = true }
		return super.playerWillDestroy(level, pos, state, player)
	}
	
	override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, randomSource: RandomSource) {
		TrackPropagator.onRailAdded(level, pos, state)
		withBlockEntityDo(level, pos) { it.tilt.undoSmoothing() }
	}
	
	override fun updateShape(
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
	
	override fun animateTick(pState: BlockState, pLevel: Level, pPos: BlockPos, pRand: Random) {}
	
	override fun onRemove(
		pState: BlockState,
		pLevel: Level,
		pPos: BlockPos,
		pNewState: BlockState,
		pIsMoving: Boolean,
	) {
		var removeBe = false
		if(pState.block != pNewState.block) {
			val blockEntity = pLevel.getBlockEntity(pPos)
			if(blockEntity is FlexiTrackBlockEntity && !pLevel.isClientSide) {
				blockEntity.willCancelDrop = blockEntity.willCancelDrop || pNewState.block == this
				blockEntity.removeInboundConnections(true)
			}
			removeBe = true
		}
		
		if(pNewState.block != this || pState != pNewState)
			TrackPropagator.onRailRemoved(pLevel, pPos, pState)
		if(removeBe)
			pLevel.removeBlockEntity(pPos)
		if(!pLevel.isClientSide)
			updateGirders(pState, pLevel, pPos, pLevel.blockTicks)
	}
	
	// No assembly on flexi track
	override fun use(
		state: BlockState,
		world: Level,
		pos: BlockPos,
		player: Player,
		hand: InteractionHand,
		hit: BlockHitResult,
	): InteractionResult {
		if(world.isClientSide) return InteractionResult.SUCCESS
		return InteractionResult.PASS
	}
	
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
	
	open val voxelShapes: FlexiTrackVoxelShapes
		get() = FlexiTrackVoxelShapes.StandardShapes
	
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
	
	@Suppress("UNCHECKED_CAST")
	override fun getBlockEntityClass(): Class<TrackBlockEntity> =
		FlexiTrackBlockEntity::class.java as Class<TrackBlockEntity>
	
	override fun getUpNormal(world: BlockGetter, pos: BlockPos, state: BlockState): Vec3 =
		flexiShape(world, pos).normal
	
	override fun getTrackAxes(world: BlockGetter, pos: BlockPos, state: BlockState): List<Vec3> =
		flexiShape(world, pos).tangents
	
	fun getTrackBase(world: BlockGetter, pos: BlockPos, state: BlockState): Vec3 =
		Vec3.atBottomCenterOf(pos).add(0.0, getElevationAtCenter(world, pos, state), 0.0)
	
	override fun getCurveStart(world: BlockGetter, pos: BlockPos, state: BlockState, axis: Vec3): Vec3 =
		getTrackBase(world, pos, state) + axis.scale(.5)
	
	override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
		val player = context.player!!
		val level = context.level
		val pos = context.clickedPos
		if(!level.isClientSide && !player.isCreative) {
			val blockEntity = level.getBlockEntity(pos)
			if(blockEntity is FlexiTrackBlockEntity) {
				blockEntity.willCancelDrop = true
				blockEntity.connections.values.forEach { it.addItemsToPlayer(player) }
			}
		}
		
		// from IWrenchable
		if(level !is ServerLevel) return InteractionResult.SUCCESS
		val event = BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player)
		MinecraftForge.EVENT_BUS.post(event)
		if(event.isCanceled) return InteractionResult.SUCCESS
		
		if(!player.isCreative) {
			val drops = getDrops(state, level, pos, level.getBlockEntity(pos), player, context.itemInHand)
			for(drop in drops) player.inventory.placeItemBackInInventory(drop)
		}
		
		state.spawnAfterBreak(level, pos, ItemStack.EMPTY, true)
		level.destroyBlock(pos, false)
		@Suppress("DEPRECATION")
		CreateSoundEvents.WRENCH_REMOVE.playOnServer(level, pos, 1f, Create.RANDOM.nextFloat() * 0.5f + 0.5f)
		return InteractionResult.SUCCESS
	}
	
	override fun overlay(world: BlockGetter, pos: BlockPos, existing: BlockState, placed: BlockState): BlockState =
		existing
	
	override fun rotate(state: BlockState, rotation: Rotation): BlockState =
		state.flexi.mapShape { it.rotate(rotation) }
	
	override fun mirror(state: BlockState, mirror: Mirror): BlockState =
		state.flexi.mapShape { it.mirror(mirror) }
	
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
			if(CompatMods.railways && bc.material == CRTrackMaterials.PHANTOM && !PhantomSpriteManager.isVisible())
				return null
			
			val t = bc.getSegmentT(bezierPoint.segment + 1).toDouble()
			val tPre = bc.getSegmentT(bezierPoint.segment).toDouble()
			val tPost = bc.getSegmentT((bezierPoint.segment + 2).coerceAtMost(bc.segmentCount)).toDouble()
			
			val offset = bc.getPosition(t)
			normal = bc.getNormal(t)
			diff = bc.getPosition(tPost)
				.subtract(bc.getPosition(tPre))
				.normalize()
			
			affine.translateBack(Vec3.atBottomCenterOf(pos))
			affine.translate(offset)
			affine.translate(0f, -4 / 16f, 0f)
			
			if(CompatMods.railways) {
				if(bc.material.trackType == CRTrackMaterials.CRTrackType.MONORAIL) {
					affine.translate(0f, 14 / 16f, 0f)
				} else {
					val casing = bc as IHasTrackCasing
					if(casing.trackCasing != null) {
						if(bc.from.y == bc.to.y) affine.translate(0f, 1 / 16f, 0f)
						else if(!casing.isAlternate) affine.translate(0f, 4 / 16f, 0f)
					}
				}
			}
		} else {
			axis = be.shape.axis1.tangent
			diff = axis.scale(direction.step.toDouble()).normalize()
			normal = getUpNormal(world, pos, state)
			
			val block = state.block
			if(CompatMods.railways && block is TrackBlock) {
				if(block.material.trackType == CRTrackMaterials.CRTrackType.MONORAIL) {
					affine.translate(0f, 14 / 16f, 0f)
				} else {
					val be = world.getBlockEntity(pos)
					if(be is TrackBlockEntity && be is IHasTrackCasing && be.trackCasing != null) {
						val spec = CRBlockPartials.TRACK_CASINGS[TrackShape.XO]
						val trackType = block.material.trackType
						if(spec != null) affine.translate(
							0f, // no x,z shift, as there is nothing like AE
							(spec.getTopSurfacePixelHeight(trackType, be.isAlternate) - 2) / 16f,
							0f
						)
					}
				}
			}
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
		
		@Suppress("REDUNDANT_ELSE_IN_WHEN")
		return when(type) {
			TrackTargetingBehaviour.RenderedTrackOverlayType.DUAL_SIGNAL -> AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER -> AllPartialModels.TRACK_OBSERVER_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.SIGNAL -> AllPartialModels.TRACK_SIGNAL_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.STATION -> AllPartialModels.TRACK_STATION_OVERLAY
			else -> { // in case other addons add other RenderedTrackOverlayTypes
				return if(affine is TransformStack<*>) {
					affine.pushPose()
					try {
						super.prepareTrackOverlay(affine, world, pos, state, bezierPoint, direction, type)
					} finally {
						affine.popPose()
					}
				} else {
					val dummy = TransformStack.of(PoseStack())
					super.prepareTrackOverlay(dummy, world, pos, state, bezierPoint, direction, type)
				}
			}
		}
	}
}
