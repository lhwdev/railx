package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.realisticSpeed.ICarriage;
import com.simibubi.create.content.trains.entity.Carriage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;


@Mixin(Carriage.class)
public class CarriageMixin implements ICarriage {
	@Shadow(remap = false) private Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> entities;
	
	@Override
	public @NotNull Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> getRailx$entities() {
		return entities;
	}
}
