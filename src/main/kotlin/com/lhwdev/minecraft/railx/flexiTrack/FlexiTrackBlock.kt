//package com.lhwdev.minecraft.railx.flexiTrack
//
//import com.simibubi.create.AllTags
//import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement
//import com.simibubi.create.content.decoration.girder.GirderBlock
//import com.simibubi.create.content.equipment.wrench.IWrenchable
//import com.simibubi.create.content.trains.graph.TrackNodeLocation
//import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
//import com.simibubi.create.content.trains.track.*
//import com.simibubi.create.foundation.block.IBE
//import com.simibubi.create.foundation.block.IHaveBigOutline
//import com.simibubi.create.foundation.block.ProperWaterloggedBlock
//import net.createmod.catnip.data.Iterate
//import net.createmod.catnip.math.VecHelper
//import net.minecraft.core.BlockPos
//import net.minecraft.core.Direction
//import net.minecraft.server.level.ServerLevel
//import net.minecraft.util.Mth
//import net.minecraft.util.RandomSource
//import net.minecraft.world.entity.LivingEntity
//import net.minecraft.world.entity.Mob
//import net.minecraft.world.entity.player.Player
//import net.minecraft.world.item.ItemStack
//import net.minecraft.world.item.context.BlockPlaceContext
//import net.minecraft.world.level.BlockGetter
//import net.minecraft.world.level.Level
//import net.minecraft.world.level.LevelAccessor
//import net.minecraft.world.level.LevelReader
//import net.minecraft.world.level.block.Block
//import net.minecraft.world.level.block.entity.BlockEntity
//import net.minecraft.world.level.block.state.BlockState
//import net.minecraft.world.level.block.state.StateDefinition
//import net.minecraft.world.level.block.state.properties.BooleanProperty
//import net.minecraft.world.level.material.FluidState
//import net.minecraft.world.level.material.PushReaction
//import net.minecraft.world.level.pathfinder.PathType
//import net.minecraft.world.phys.Vec3
//import net.minecraft.world.phys.shapes.CollisionContext
//import net.minecraft.world.phys.shapes.Shapes
//import net.minecraft.world.phys.shapes.VoxelShape
//import net.minecraft.world.ticks.LevelTickAccess
//import java.util.*
//import java.util.function.BiFunction
//import java.util.function.Consumer
//import java.util.function.Function
//import kotlin.math.atan2
//
//
//class FlexiTrackBlock(properties: Properties, val material: TrackMaterial) : Block(properties),
//    IBE<FlexiTrackBlockEntity>,
//    IWrenchable,
//    ITrackBlock,
//    SpecialBlockItemRequirement,
//    ProperWaterloggedBlock,
//    IHaveBigOutline {
//    companion object {
//        val BaseDirection = FlexiDirectionProperty.create("direction")
//        val HasBe: BooleanProperty = TrackBlock.HAS_BE
//        val Waterlogged: BooleanProperty = TrackBlock.WATERLOGGED
//    }
//
//    init {
//        registerDefaultState(
//            defaultBlockState()
//                .setValue(BaseDirection, FlexiDirection.Known.Divisions[0])
//                .setValue(HasBe, true)
//                .setValue(Waterlogged, false)
//        )
//    }
//
//    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
//        super.createBlockStateDefinition(builder.add(BaseDirection, HasBe, Waterlogged))
//    }
//
//
//    fun flexiShape(world: BlockGetter, pos: BlockPos): FlexiShape =
//        world.getBlockEntity(pos) as FlexiShape
//
//
//    override fun getBlockPathType(state: BlockState, level: BlockGetter, pos: BlockPos, mob: Mob?): PathType =
//        PathType.RAIL
//
//    override fun getFluidState(state: BlockState): FluidState =
//        fluidState(state)
//
//    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
//        val stateForPlacement = withWater(super.getStateForPlacement(context), context)
//        val player = context.player
//        if (player == null) return stateForPlacement
//
//        var lookAngle = player.lookAngle.multiply(1.0, 0.0, 1.0)
//        if (Mth.equal(lookAngle.length(), 0.0)) {
//            lookAngle = VecHelper.rotate(Vec3(0.0, 0.0, 1.0), -player.yRot.toDouble(), Direction.Axis.Y)
//        }
//
//        return stateForPlacement.setValue(
//            BaseDirection,
//            FlexiDirection.Known.roundFromAngle(atan2(lookAngle.x, lookAngle.z))
//        )
//    }
//
//    override fun getPistonPushReaction(pState: BlockState): PushReaction =
//        PushReaction.BLOCK
//
//    override fun playerWillDestroy(pLevel: Level, pPos: BlockPos, pState: BlockState, pPlayer: Player): BlockState {
//        super.playerWillDestroy(pLevel, pPos, pState, pPlayer)
//
//        if (pLevel.isClientSide) return pState
//        if (!pPlayer.isCreative) return pState
//        withBlockEntityDo(pLevel, pPos) { be ->
//            be.cancelDrops = true
//            be.removeInboundConnections(true)
//        }
//
//        return pState
//    }
//
//    public override fun onPlace(
//        pState: BlockState,
//        pLevel: Level,
//        pPos: BlockPos,
//        pOldState: BlockState,
//        pIsMoving: Boolean
//    ) {
//        if (pState === pOldState) return
//        if (pLevel.isClientSide) return
//        val blockTicks = pLevel.blockTicks
//        if (!blockTicks.hasScheduledTick(pPos, this)) pLevel.scheduleTick(pPos, this, 1)
//        updateGirders(pState, pLevel, pPos, blockTicks)
//    }
//
//    override fun setPlacedBy(
//        pLevel: Level,
//        pPos: BlockPos,
//        pState: BlockState,
//        pPlacer: LivingEntity?,
//        pStack: ItemStack
//    ) {
//        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack)
//        withBlockEntityDo(pLevel, pPos) { it.validateConnections() }
//    }
//
//    public override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, randomSource: RandomSource) {
//        TrackPropagator.onRailAdded(level, pos, state)
//        withBlockEntityDo(level, pos) { it.tilt.undoSmoothing() }
//    }
//
//    public override fun updateShape(
//        state: BlockState, pDirection: Direction, pNeighborState: BlockState,
//        level: LevelAccessor, pCurrentPos: BlockPos, pNeighborPos: BlockPos
//    ): BlockState {
//        updateWater(level, state, pCurrentPos)
//        return state
//    }
//
//    override fun getYOffsetAt(world: BlockGetter, pos: BlockPos, state: BlockState?, end: Vec3?): Int =
//        getBlockEntity(world, pos)?.tilt?.getYOffsetForAxisEnd(end) ?: 0
//
//    override fun getConnected(
//        worldIn: BlockGetter,
//        pos: BlockPos,
//        state: BlockState,
//        linear: Boolean,
//        connectedTo: TrackNodeLocation?
//    ): MutableCollection<DiscoveredLocation> {
//        TODO()
//        val list: MutableCollection<DiscoveredLocation>
//        val world = if (connectedTo != null && worldIn is ServerLevel) {
//            worldIn.server.getLevel(connectedTo.dimension)
//        } else {
//            worldIn
//        }
//
//        if (getTrackAxes(world, pos, state).size > 1) {
//            list = mutableListOf<DiscoveredLocation>()
//            val center = Vec3.atBottomCenterOf(pos)
//                .add(0.0, getElevationAtCenter(world, pos, state), 0.0)
//
//            val shape = state.getValue(TrackBlock.SHAPE)
//            for (axis in getTrackAxes(world, pos, state)) {
//                for (fromCenter in Iterate.trueAndFalse) ITrackBlock.addToListIfConnected(
//                    connectedTo,
//                    list,
//                    { d, b ->
//                        axis.scale((if (b) 0.0 else if (fromCenter) -d else d))
//                            .add(center)
//                    },
//                    { b -> shape.normal },
//                    { b: Boolean? -> if (world is Level) world.dimension() else Level.OVERWORLD },
//                    { v: Vec3? -> 0 },
//                    axis,
//                    null,
//                    { b: Boolean?, v: Vec3? -> ITrackBlock.getMaterialSimple(world, v) })
//            }
//        } else list = super.getConnected(world, pos, state, linear, connectedTo)
//
//        if (!state.getValue<Boolean?>(TrackBlock.HAS_BE)) return list
//        if (linear) return list
//
//        val blockEntity = world!!.getBlockEntity(pos)
//        if (blockEntity !is TrackBlockEntity) return list
//
//        val connections = blockEntity.getConnections()
//        connections.forEach { (connectedPos: BlockPos?, bc: BezierConnection?) ->
//            ITrackBlock.addToListIfConnected(
//                connectedTo,
//                list,
//                BiFunction { d: Double?, b: Boolean? ->
//                    if (d == 1.0) Vec3.atLowerCornerOf(bc!!.bePositions.get(b!!)) else bc!!.starts.get(
//                        b!!
//                    )
//                },
//                Function { first: Boolean? -> bc!!.normals.get(first!!) },
//                Function { b: Boolean? -> if (world is Level) world.dimension() else Level.OVERWORLD },
//                Function { end: Vec3? -> bc!!.yOffsetAt(end) },
//                null,
//                bc,
//                BiFunction { b: Boolean?, v: Vec3? -> ITrackBlock.getMaterialSimple(world, v, bc!!.getMaterial()) })
//        }
//
//        if (blockEntity.boundLocation == null || world !is ServerLevel) return list
//
//        val otherDim = blockEntity.boundLocation.getFirst()
//        val otherLevel = world.getServer()
//            .getLevel(otherDim)
//        if (otherLevel == null) return list
//        val boundPos = blockEntity.boundLocation.getSecond()
//        val boundState = otherLevel.getBlockState(boundPos)
//        if (!AllTags.AllBlockTags.TRACKS.matches(boundState)) return list
//
//        val center = Vec3.atBottomCenterOf(pos)
//            .add(0.0, getElevationAtCenter(world, pos, state), 0.0)
//        val boundCenter = Vec3.atBottomCenterOf(boundPos)
//            .add(0.0, getElevationAtCenter(otherLevel, boundPos, boundState), 0.0)
//        val shape = state.getValue<TrackShape?>(TrackBlock.SHAPE)
//        val boundShape = boundState.getValue<TrackShape?>(TrackBlock.SHAPE)
//        val boundAxis = getTrackAxes(otherLevel, boundPos, boundState).get(0)
//
//        getTrackAxes(world, pos, state).forEach(Consumer { axis: Vec3? ->
//            ITrackBlock.addToListIfConnected(
//                connectedTo,
//                list,
//                BiFunction { d: Double?, b: Boolean? ->
//                    (if (b) axis else boundAxis)!!.scale(d!!)
//                        .add(if (b) center else boundCenter)
//                },
//                Function { b: Boolean? -> (if (b) shape else boundShape).getNormal() },
//                Function { b: Boolean? -> if (b) world.dimension() else otherLevel.dimension() },
//                Function { v: Vec3? -> 0 },
//                axis,
//                null,
//                BiFunction { b: Boolean?, v: Vec3? -> ITrackBlock.getMaterialSimple(if (b) world else otherLevel, v) })
//        })
//
//        return list
//    }
//
//    public override fun onRemove(
//        pState: BlockState,
//        pLevel: Level,
//        pPos: BlockPos,
//        pNewState: BlockState,
//        pIsMoving: Boolean
//    ) {
//        var removeBE = false
//        if (!pState.`is`(pNewState.block)) {
//            val blockEntity = pLevel.getBlockEntity(pPos)
//            if (blockEntity is FlexiTrackBlockEntity && !pLevel.isClientSide) {
//                blockEntity.removeInboundConnections(true)
//            }
//            removeBE = true
//        }
//
//        if (pNewState.block !== this || pState !== pNewState) {
//            TrackPropagator.onRailRemoved(pLevel, pPos, pState)
//        }
//        if (removeBE) {
//            pLevel.removeBlockEntity(pPos)
//        }
//        if (!pLevel.isClientSide) {
//            updateGirders(pState, pLevel, pPos, pLevel.blockTicks)
//        }
//    }
//
//    // No assembly on flexi track
////    override fun useItemOn(
////        stack: ItemStack,
////        state: BlockState,
////        level: Level,
////        pos: BlockPos,
////        player: Player,
////        hand: InteractionHand,
////        hitResult: BlockHitResult
////    ): ItemInteractionResult {
////        if (level.isClientSide) return ItemInteractionResult.SUCCESS
////        for (entry in StationBlockEntity.assemblyAreas.get(level).entries) {
////            if (!entry.value.isInside(pos)) continue
////            val station = level.getBlockEntity(entry.key)
////            if (station is StationBlockEntity && station.trackClicked(player, hand, this, state, pos))
////                return ItemInteractionResult.SUCCESS
////        }
////
////        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
////    }
//
//    private fun updateGirders(pState: BlockState, pLevel: Level, pPos: BlockPos, blockTicks: LevelTickAccess<Block?>) {
//        for (axis in getTrackAxes(pLevel, pPos, pState)) {
//            if (axis.length() > 1 || axis.y != 0.0) continue
//            for (side in Iterate.positiveAndNegative) {
//                val girderPos = pPos.below()
//                    .offset(BlockPos.containing(axis.z * side, 0.0, axis.x * side))
//                val girderState = pLevel.getBlockState(girderPos)
//                val block = girderState.block
//                if (block is GirderBlock && !blockTicks.hasScheduledTick(girderPos, block)) {
//                    pLevel.scheduleTick(girderPos, block, 1)
//                }
//            }
//        }
//    }
//
//    public override fun canSurvive(state: BlockState, reader: LevelReader, pos: BlockPos): Boolean {
//        return reader.getBlockState(pos.below()).block !== this
//    }
//
//    public override fun getShape(
//        state: BlockState,
//        world: BlockGetter,
//        pos: BlockPos,
//        p_60558_: CollisionContext
//    ): VoxelShape {
//        return getFullShape(flexiShape(world, pos))
//    }
//
//    public override fun getInteractionShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape {
//        return getFullShape(flexiShape(world, pos))
//    }
//
//    private fun getFullShape(shape: FlexiShape): VoxelShape {
//        return if (shape is FlexiShape.Single && shape.axe is FlexiDirection.Known) {
//            FlexiTrackVoxelShapes.known(shape.axe)
//        } else {
//            // TODO: cache
//            shape.axes.fold(Shapes.empty()) { acc, axis ->
//                Shapes.or(acc, FlexiTrackVoxelShapes.createShape(axis.tangent))
//            }
//        }
//    }
//
//    override fun getCollisionShape(
//        state: BlockState,
//        level: BlockGetter,
//        pos: BlockPos,
//        context: CollisionContext
//    ): VoxelShape = Shapes.empty()
//
////    override fun newBlockEntity(p_153215_: BlockPos, p_153216_: BlockState): BlockEntity? {
////
////    }
//}
