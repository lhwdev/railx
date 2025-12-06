package com.lhwdev.build.minecraft.utils

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration


fun NamedDomainObjectProvider<Configuration>.extendsFrom(other: NamedDomainObjectProvider<Configuration>) {
	if(name == other.name) return
	configure { extendsFrom(other.get()) }
}
