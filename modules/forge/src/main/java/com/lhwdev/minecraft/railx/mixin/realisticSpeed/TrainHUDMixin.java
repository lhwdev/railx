package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.simibubi.create.content.trains.TrainHUD;
import org.spongepowered.asm.mixin.Mixin;


// TODO: implement brake UI; press , to decrement, . to increment braking level.

@Mixin(value = TrainHUD.class, remap = false)
public class TrainHUDMixin {
}
