package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.realisticSpeed.IContraptionBlockEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Contraption.class)
public abstract class ContraptionMixin implements IContraptionBlockEntity {
	@Unique
	private boolean railx$requiresBlockEntity = false;
	
	@Override
	public boolean getRequiresBlockEntity() {
		return railx$requiresBlockEntity;
	}
	
	@Override
	public void setRequiresBlockEntity(boolean b) {
		railx$requiresBlockEntity = b;
	}
	
	@Override
	@Invoker("readBlockEntity")
	public abstract @Nullable BlockEntity onReadBlockEntity(
		@NotNull Level level,
		@NotNull StructureTemplate.StructureBlockInfo info,
		@NotNull CompoundTag tag
	);
	
	@Redirect(method = "lambda$readBlocksCompound$15", at = @At(value = "FIELD", target = "Lnet/minecraft/world" +
		"/level" +
		"/Level;isClientSide:Z", opcode = Opcodes.GETFIELD))
	boolean willLoadBlockEntities(Level instance) {
		return railx$requiresBlockEntity || instance.isClientSide;
	}
}
