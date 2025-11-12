package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.realisticSpeed.ClientSideContraptionBlockEntities;
import com.lhwdev.minecraft.railx.realisticSpeed.ContraptionBlockEntities;
import com.lhwdev.minecraft.railx.realisticSpeed.ContraptionWithBlockEntity;
import com.lhwdev.minecraft.railx.realisticSpeed.ServerSideContraptionBlockEntities;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;


@Mixin(Contraption.class)
public abstract class ContraptionMixin implements ContraptionWithBlockEntity {
	@Shadow
	public abstract ClientContraption getOrCreateClientContraptionLazy();
	
	@Unique
	private ServerSideContraptionBlockEntities railx$serverSideBlockEntities;
	
	@Override
	public @NotNull ServerSideContraptionBlockEntities railx$serverSideBlockEntities(@NotNull Level level) {
		if(railx$serverSideBlockEntities == null) {
			railx$serverSideBlockEntities = new ServerSideContraptionBlockEntities((Contraption) (Object) this, level);
		}
		return railx$serverSideBlockEntities;
	}
	
	@Override
	public @NotNull ContraptionBlockEntities railx$blockEntities(@NotNull Level level) {
		if(level.isClientSide) {
			return new ClientSideContraptionBlockEntities(getOrCreateClientContraptionLazy());
		} else {
			return railx$serverSideBlockEntities(level);
		}
	}
}
