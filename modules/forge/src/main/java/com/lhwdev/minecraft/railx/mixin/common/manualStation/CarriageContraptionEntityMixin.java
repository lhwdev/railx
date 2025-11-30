package com.lhwdev.minecraft.railx.mixin.common.manualStation;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.common.ManualStation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.station.GlobalStation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;


@Mixin(value = CarriageContraptionEntity.class)
public abstract class CarriageContraptionEntityMixin {
	@Shadow(remap = false) private Carriage carriage;
	
	
	@Shadow(remap = false)
	protected abstract void sendPrompt(Player player, MutableComponent component, boolean shadow);
	
	@Unique
	private boolean railx$manualStationMessage;
	
	
	@WrapOperation(method = "control", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/entity/Navigation;findNearestApproachable(Z)Lcom/simibubi/create/content/trains/station/GlobalStation;",
		remap = false), remap = false)
	GlobalStation findNearestApproachable(Navigation instance, boolean forward, Operation<GlobalStation> original) {
		if(!RailXConfig.Server.Value.getCommon().getManualStation().get())
			return original.call(instance, forward);
		
		return null;
	}
	
	@Inject(method = "control", at = @At(value = "TAIL"), remap = false)
	void onControl(
		BlockPos controlsLocalPos,
		Collection<Integer> heldControls,
		Player player,
		CallbackInfoReturnable<Boolean> cir,
		@Local(index = 6) boolean inverted,
		@Local(index = 7) int targetSpeed
	) {
		if(!RailXConfig.Server.Value.getCommon().getManualStation().get()) return;
		
		var train = carriage.train;
		var directedSpeed = targetSpeed != 0 ? targetSpeed : carriage.train.speed;
		var forward = !carriage.train.doubleEnded || (directedSpeed != 0 ? directedSpeed > 0 : !inverted);
		var result = ManualStation.INSTANCE.findApproachingStation(train, forward, !inverted);
		if(result == null) {
			if(railx$manualStationMessage) {
				player.displayClientMessage(CommonComponents.EMPTY, true);
				railx$manualStationMessage = false;
			}
			return;
		}
		
		double distanceLimit = RailXConfig.Server.Value.getCommon().getManualStationDistanceLimit().get();
		
		GlobalStation station = result.getStation();
		double distance = result.getDistance();
		double distanceAbs = Math.abs(distance);
		var isClose = distanceAbs <= distanceLimit;
		boolean spaceDown = heldControls.contains(4);
		boolean slow = Math.abs(train.speed) < 0.001;
		if(isClose && spaceDown && slow) {
			station.reserveFor(train);
			train.arriveAt(station);
			if(railx$manualStationMessage) { // ??
				player.displayClientMessage(CommonComponents.EMPTY, true);
				railx$manualStationMessage = false;
			}
		} else if(train.currentStation == null) {
			MutableComponent message;
			if(isClose) message = Component.literal(slow ? "Press " : "Stop and press ")
				.append(Component.keybind("key.jump"))
				.append(" to arrive at ")
				.append(station.name);
			else {
				message = Component.literal("Approaching ")
					.append(station.name);
				if(distanceAbs <= distanceLimit + 2) message
					.append(": ")
					.append(distance > 0 ? "-" : "+")
					.append(Long.toString(Math.abs(Math.round(distanceAbs * 10) * 100)));
			}
			sendPrompt(player, message, false);
			railx$manualStationMessage = true;
		}
	}
}
