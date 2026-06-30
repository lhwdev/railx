package com.lhwdev.minecraft.railx.mixin.compat.pantographsandwires;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.compat.pantographsandwires.AutoCantilever;
import com.lhwdev.minecraft.railx.compat.pantographsandwires.AutoCantileverApplier;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(CantileverBlock.class)
public abstract class CantileverBlockMixin extends AbstractCantileverBlock {
	public CantileverBlockMixin(Properties properties) {super(properties);}
	
	@Override
	public InteractionResult use(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit
	) {
		ItemStack stack = player.getItemInHand(hand);
		if(level.isClientSide && stack.isEmpty() && level.getBlockEntity(pos) instanceof CantileverBlockEntity be) {
			if(!RailXConfig.Client.Value.getCompat().getPantographsAndWiresAutoCantilever().get())
				return super.use(state, level, pos, player, hand, hit);
			AutoCantilever autoCantilever = new AutoCantilever(8.5f);
			AutoCantileverApplier.INSTANCE.modifyBlockEntityData(be, autoCantilever);
			return InteractionResult.SUCCESS;
		}
		return super.use(state, level, pos, player, hand, hit);
	}
}
