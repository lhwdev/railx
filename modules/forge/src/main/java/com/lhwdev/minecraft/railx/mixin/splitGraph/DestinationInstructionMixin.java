package com.lhwdev.minecraft.railx.mixin.splitGraph;

import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(DestinationInstruction.class)
public class DestinationInstructionMixin {
	//	@Redirect(method = "start", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity" +
	//		"/Train;graph:Lcom/simibubi/create/content/trains/graph/TrackGraph;"))
	//	TrackGraph getGraphForStart(Train train) {
	//		return new AllConnectedTrackGraphs(
	//			Create.RAILWAYS.trackNetworks,
	//			train.graph instanceof MergedTrackGraph merged ? merged.getBase() : train.graph
	//		);
	//		//		return SplitUtils.getEmptyTrackGraph();
	//	}
	//
	//	//	@ModifyArg(method = "start", at = @At(value = "INVOKE", target =
	//	"Lcom/simibubi/create/content/trains/entity" +
	//	//		"/Navigation;findPathTo(Ljava/util/ArrayList;D)
	//	Lcom/simibubi/create/content/trains/graph/DiscoveredPath;"),
	//	//		index = 0)
	//	//	ArrayList<GlobalStation> getStations(
	//	//		ArrayList<GlobalStation> destinations,
	//	//		@Local(index = 3) String regex,
	//	//		@Share("anyMatch") LocalBooleanRef anyMatchRef
	//	//	) {
	//	//		return new SlotObjects.StationPredicate((station) -> {
	//	//			if(!station.name.matches(regex)) return false;
	//	//			anyMatchRef.set(true);
	//	//			return true;
	//	//		});
	//	//	}
	//
	//	@Inject(method = "start", at = @At(value = "INVOKE_ASSIGN", target =
	//	"Lcom/simibubi/create/content/trains/entity" +
	//		"/Navigation;findPathTo(Ljava/util/ArrayList;D)Lcom/simibubi/create/content/trains/graph/DiscoveredPath;"))
	//	void afterFindPathTo(
	//		CallbackInfoReturnable<DiscoveredPath> cir,
	//		@Local(index = 4) LocalBooleanRef anyMatch,
	//		@Share("anyMatch") LocalBooleanRef anyMatchRef
	//	) {
	//		if(anyMatchRef.get()) anyMatch.set(true);
	//	}
}
