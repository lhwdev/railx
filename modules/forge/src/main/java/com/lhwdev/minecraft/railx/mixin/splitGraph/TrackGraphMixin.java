package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.lhwdev.minecraft.railx.splitGraph.*;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;


@SuppressWarnings("DataFlowIssue")
@Mixin(TrackGraph.class)
public abstract class TrackGraphMixin implements TrackGraphForSplit {
	@Shadow(remap = false)
	public UUID id;
	
	@Shadow(remap = false)
	public abstract void invalidateBounds();
	
	@Shadow(remap = false)
	public abstract void markDirty();
	
	@Shadow(remap = false)
	public abstract void addNode(TrackNode node);
	
	@Shadow(remap = false)
	Map<TrackNodeLocation, TrackNode> nodes;
	
	
	/// Managing SplittingGraphNode
	
	@Unique
	private @Nullable UUID railx$connectedId;
	
	// does not need to serialize --- every time it deserializes, addNode / addNodeIfAbsent is called
	@Unique // TrackGraph.id -> setOf(SplittingTrackNode.netId ...)
	private Map<UUID, IntArraySet> railx$connectedGraphs;
	
	@Unique
	private Set<TrackGraph> railx$allConnectedGraphs;
	@Unique
	private boolean railx$allConnectedGraphsInvalid;
	
	@Override
	public @Nullable UUID railx$getConnectedId() {
		return railx$connectedId;
	}
	
	@Override
	public void railx$setConnectedId(@Nullable UUID id) {
		railx$connectedId = id;
	}
	
	@Unique
	private void railx$markAllConnectedInvalid() {
		if(railx$allConnectedGraphs != null) {
			for(var graph : railx$allConnectedGraphs)
				((TrackGraphMixin) (Object) graph).railx$allConnectedGraphsInvalid = true;
		}
		railx$allConnectedGraphsInvalid = true;
	}
	
	
	@Inject(method = "<init>(Ljava/util/UUID;)V", at = @At("RETURN"), remap = false)
	void onInitialize(CallbackInfo ci) {
		railx$connectedGraphs = new HashMap<>();
	}
	
	@Inject(method = "addNode", at = @At("TAIL"), remap = false)
	void onAddNode(TrackNode node, CallbackInfo ci) {
		if(node instanceof SplittingTrackNode split) {
			var splits = railx$connectedGraphs.computeIfAbsent(split.getOtherGraph(), (id) -> new IntArraySet());
			splits.add(split.getNetId());
			railx$markAllConnectedInvalid();
			var otherGraph = Create.RAILWAYS.trackNetworks.get(split.getOtherGraph());
			if(otherGraph != null) ((TrackGraphMixin) (Object) otherGraph).railx$allConnectedGraphsInvalid = true;
		}
	}
	
	@Inject(method = "addNodeIfAbsent", at = @At("TAIL"), remap = false)
	void onAddNodeIfAbsent(TrackNode node, CallbackInfoReturnable<Boolean> cir) {
		if(node instanceof SplittingTrackNode split) {
			var splits = railx$connectedGraphs.computeIfAbsent(split.getOtherGraph(), (id) -> new IntArraySet());
			splits.add(split.getNetId());
			railx$markAllConnectedInvalid();
			var otherGraph = Create.RAILWAYS.trackNetworks.get(split.getOtherGraph());
			if(otherGraph != null) ((TrackGraphMixin) (Object) otherGraph).railx$allConnectedGraphsInvalid = true;
		}
	}
	
	@Inject(method = "removeNode", at = @At("HEAD"), remap = false)
	void onRemoveNode(LevelAccessor level, TrackNodeLocation location, CallbackInfoReturnable<Boolean> cir) {
		TrackNode node = nodes.get(location);
		if(node instanceof SplittingTrackNode split) {
			UUID other = split.getOtherGraph();
			var splits = railx$connectedGraphs.get(other);
			if(splits == null) return;
			splits.remove(split.getNetId());
			if(splits.isEmpty()) {
				railx$markAllConnectedInvalid();
				railx$connectedGraphs.remove(other);
				TrackGraphConnectedId.INSTANCE.connectedGraphRemoved((TrackGraph) (Object) this, other);
			}
		}
	}
	
	@Definition(id = "train", local = @Local(type = Train.class, index = 7))
	@Definition(id = "graph", field = "Lcom/simibubi/create/content/trains/entity/Train;" +
		"graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;")
	@Expression("train.graph != this")
	@ModifyExpressionValue(method = "removeNode", at = @At(value = "MIXINEXTRAS:EXPRESSION", remap = false),
		remap = false)
	boolean isTrainReachableForRemoveNode(boolean original, @Local(index = 7) Train train) {
		if(train.graph instanceof MergedTrackGraph merged) {
			return !MergedTrackGraph.contains(merged, (TrackGraph) (Object) this);
		}
		return original;
	}
	
	@Inject(method = "transfer", at = @At("RETURN"), remap = false)
	void onTransfer(LevelAccessor level, TrackNode node, TrackGraph target, CallbackInfo ci) {
		if(node instanceof SplittingTrackNode split) {
			var otherId = split.getOtherGraph();
			var splits = railx$connectedGraphs.get(otherId);
			if(splits == null) return;
			splits.remove(split.getNetId());
			if(splits.isEmpty()) {
				railx$markAllConnectedInvalid();
				railx$connectedGraphs.remove(otherId);
				TrackGraphConnectedId.INSTANCE.connectedGraphRemoved((TrackGraph) (Object) this, otherId);
			}
		}
	}
	
	@Inject(method = "transferAll", at = @At("RETURN"), remap = false)
	void onTransferAll(TrackGraph graph, CallbackInfo ci) {
		for(Train train : Create.RAILWAYS.trains.values()) {
			if(!(train.graph instanceof MergedTrackGraph merged)) continue;
			if(merged.getGraphs().contains((TrackGraph) (Object) this)) {
				var graphs = new ArrayList<>(merged.getGraphs());
				for(var i = 0; i < graphs.size(); i++) {
					if(graphs.get(i) == (Object) this) graphs.set(i, graph);
				}
				train.graph = new MergedTrackGraphImpl(graphs);
			}
		}
		
		TrackGraphConnectedId.INSTANCE.transferAll((TrackGraph) (Object) this, graph);
		railx$markAllConnectedInvalid();
		railx$connectedGraphs.clear();
	}
	
	
	/// Saving & Loading SplittingGraphNode
	
	@Override
	public void railx$addCreatedNode(@NotNull TrackNode node) {
		addNode(node);
		Create.RAILWAYS.sync.nodeAdded((TrackGraph) (Object) this, node);
		invalidateBounds();
		markDirty();
	}
	
	@Unique
	public void railx$loadSplittingNode(CompoundTag tag, TrackNodeLocation location, int netId, Vec3 normal) {
		addNode(SplittingTrackNode.readSplit(new TrackNode(location, netId, normal), tag));
	}
	
	@Override
	public void railx$loadSplittingNode(
		@NotNull SplittingTrackNode.Data data,
		@NotNull TrackNodeLocation location,
		int netId,
		@NotNull Vec3 normal
	) {
		addNode(data.toNode(location, netId, normal));
	}
	
	@Override
	public @NotNull Collection<UUID> railx$connectedGraphs() {
		return railx$connectedGraphs.keySet();
	}
	
	@Override
	public @NotNull Set<TrackGraph> railx$allConnectedGraphsIncludingSelf(@NotNull GlobalRailwayManager manager) {
		if(railx$allConnectedGraphs == null) {
			railx$allConnectedGraphs = TrackGraphForSplitUtils.allConnectedGraphsIncludingSelfImpl(
				(TrackGraph) (Object) this,
				manager
			);
			railx$allConnectedGraphsInvalid = false;
			for(var connected : railx$allConnectedGraphs) {
				((TrackGraphMixin) (Object) connected).railx$allConnectedGraphs = railx$allConnectedGraphs;
				((TrackGraphMixin) (Object) connected).railx$allConnectedGraphsInvalid = false;
			}
		} else if(railx$allConnectedGraphsInvalid) {
			var updated = TrackGraphForSplitUtils.allConnectedGraphsIncludingSelfImpl(
				(TrackGraph) (Object) this,
				manager
			);
			
			for(var removed : railx$allConnectedGraphs) {
				if(updated.contains(removed)) continue;
				((TrackGraphMixin) (Object) removed).railx$allConnectedGraphsInvalid = true;
			}
			for(var added : updated) { // allConnected is commutative (x in y.allConnected <=> y in x.allConnected)
				((TrackGraphMixin) (Object) added).railx$allConnectedGraphs = updated;
				((TrackGraphMixin) (Object) added).railx$allConnectedGraphsInvalid = false;
			}
			railx$allConnectedGraphs = updated;
			railx$allConnectedGraphsInvalid = false;
		}
		return railx$allConnectedGraphs;
	}
	
	@Inject(method = "write", at = @At("RETURN"), remap = false)
	void onWrite(CallbackInfoReturnable<CompoundTag> cir) {
		if(railx$connectedId != null)
			cir.getReturnValue().putUUID("railx:ConnectedId", railx$connectedId);
	}
	
	@Inject(method = "write", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/nbt/ListTag;add" +
		"(Ljava/lang/Object;)Z", ordinal = 0, remap = false), remap = false)
	void onWriteGraphNode(
		CallbackInfoReturnable<CompoundTag> cir,
		@Local ListTag nodesList,
		@Local TrackNode node
	) {
		if(!(node instanceof SplittingTrackNode splitting)) return;
		CompoundTag nodeTag = (CompoundTag) nodesList.get(nodesList.size() - 1);
		nodeTag.put("railx:Splitting", splitting.writeSplit());
	}
	
	
	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void onRead(
		CompoundTag tag,
		DimensionPalette dimensions,
		CallbackInfoReturnable<TrackGraph> cir
	) {
		if(tag.contains("railx:ConnectedId")) {
			((TrackGraphMixin) (Object) cir.getReturnValue()).railx$connectedId =
				tag.getUUID("railx:ConnectedId");
		}
	}
	
	@WrapOperation(method = "read", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph" +
		"/TrackGraph;loadNode(Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;" +
		"ILnet/minecraft/world/phys/Vec3;)V", remap = false), remap = false)
	private static void onReadGraphNode(
		TrackGraph instance,
		TrackNodeLocation location, int netId, Vec3 normal,
		Operation<Void> original,
		@Local(ordinal = 1) CompoundTag nodeTag
	) {
		if(nodeTag.contains("railx:Splitting")) {
			var splittingTag = nodeTag.getCompound("railx:Splitting");
			((TrackGraphMixin) (Object) instance).railx$loadSplittingNode(splittingTag, location, netId, normal);
		} else if(nodeTag.contains("Splitting")) { // TODO: temporary compatibility with my old maps; to remove
			var splittingTag = nodeTag.getCompound("Splitting");
			((TrackGraphMixin) (Object) instance).railx$loadSplittingNode(splittingTag, location, netId, normal);
		} else {
			original.call(instance, location, netId, normal);
		}
	}
	
	
	/// Updating SplittingTrackNode.otherGraph
	
	@Inject(method = "transfer", at = @At("RETURN"), remap = false)
	void updateSplitOnTransfer(LevelAccessor level, TrackNode node, TrackGraph target, CallbackInfo ci) {
		if(node instanceof SplittingTrackNode split) {
			var otherGraph = Create.RAILWAYS.trackNetworks.get(split.getOtherGraph());
			if(otherGraph == null) return;
			var otherNode = otherGraph.locateNode(node.getLocation());
			if(!(otherNode instanceof SplittingTrackNode otherSplit)) return;
			if(otherSplit.getOtherGraph().equals(id)) {
				otherSplit.setOtherGraph(target.id);
				SplittingTrackNodeSync.INSTANCE.nodeOtherGraphChanged(otherGraph, otherSplit);
			}
		}
	}
	
	@Inject(method = "transferAll", at = @At("HEAD"), remap = false)
	void updateSplitOnTransferAll(TrackGraph to, CallbackInfo ci) {
		for(var node : nodes.values()) {
			if(!(node instanceof SplittingTrackNode split)) continue;
			var otherGraph = Create.RAILWAYS.trackNetworks.get(split.getOtherGraph());
			if(otherGraph == null) continue;
			var otherNode = otherGraph.locateNode(node.getLocation());
			if(!(otherNode instanceof SplittingTrackNode otherSplit)) continue;
			if(otherSplit.getOtherGraph().equals(id)) {
				otherSplit.setOtherGraph(to.id);
				SplittingTrackNodeSync.INSTANCE.nodeOtherGraphChanged(otherGraph, otherSplit);
			}
		}
	}
}
