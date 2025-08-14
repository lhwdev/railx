package com.lhwdev.minecraft.railx.utils

import net.createmod.catnip.data.Pair


operator fun <A> Pair<A, *>.component1(): A = first
operator fun <B> Pair<*, B>.component2(): B = second
