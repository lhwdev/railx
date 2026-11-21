package com.lhwdev.minecraft.railx.modDevStandalone.debug;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;


/**
 * Simple fix for common `cannot access module` issue of kotlinx.coroutines.
 */
public final class CoroutineDebugModuleAgent {
	public static void premain(String arguments, Instrumentation instrumentation) {
		instrumentation.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(
				Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
				ProtectionDomain protectionDomain, byte[] classfileBuffer
			) {
				if(!"kotlin/coroutines/jvm/internal/DebugProbesKt".equals(className) ||
					!"kotlin.stdlib".equals(module.getName())) return null;
				
				var layer = module.getLayer();
				if(layer == null) return null;
				
				layer.findModule("kotlinx.coroutines.core").ifPresent(coroutines -> {
					if(!module.canRead(coroutines)) {
						instrumentation.redefineModule(
							module, Set.of(coroutines), Map.of(), Map.of(), Set.of(), Map.of()
						);
					}
				});
				return null;
			}
		});
	}
}
