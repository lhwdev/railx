package com.lhwdev.minecraft.railx.mixin.trainMap;

import com.lhwdev.minecraft.railx.trainMap.GuiMapInit;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;
import xaero.map.gui.ScreenBase;


@Mixin(GuiMap.class)
public class GuiMapMixin extends ScreenBase {
	protected GuiMapMixin(Screen parent, Screen escape, Component titleIn) {super(parent, escape, titleIn);}
	
	@Inject(method = "init", at = @At("HEAD"))
	void onInit(CallbackInfo ci) {
		GuiMapInit.INSTANCE.init((GuiMap) (Object) this);
	}
}
