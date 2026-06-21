package com.lhwdev.minecraft.railx.mixin.compat.pantographsandwires;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.compat.pantographsandwires.AutoCantilever;
import com.lhwdev.minecraft.railx.compat.pantographsandwires.AutoCantileverApplier;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(CantileverBlock.class)
public abstract class CantileverBlockMixin extends AbstractCantileverBlock {
	public CantileverBlockMixin(Properties properties) {super(properties);}
	
	@Override
	protected @NotNull ItemInteractionResult useItemOn(
		@NotNull ItemStack stack,
		@NotNull BlockState state,
		@NotNull Level level,
		@NotNull BlockPos pos,
		@NotNull Player player,
		@NotNull InteractionHand hand,
		@NotNull BlockHitResult hitResult
	) {
		if(level.isClientSide && stack.isEmpty() && level.getBlockEntity(pos) instanceof CantileverBlockEntity be) {
			if(RailXConfig.Client.Value.getCompat().getPantographsAndWiresAutoCantilever().isFalse())
				return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
			AutoCantilever autoCantilever = new AutoCantilever(8.5f);
			AutoCantileverApplier.INSTANCE.modifyBlockEntityData(be, autoCantilever);
			return ItemInteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}
}
