package com.lhwdev.build.minecraft.utils

import com.github.jengelman.gradle.plugins.shadow.tasks.DependencyFilter
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import java.io.Serializable


open class ProjectOnlyDependencyFilter(@Transient private val project: Project) :
	DependencyFilter, Serializable {
	override fun resolve(configuration: Configuration): FileCollection =
		configuration.incoming.artifactView { componentFilter { it is ProjectComponentIdentifier } }
			.files
	
	override fun resolve(configurations: Collection<Configuration>): FileCollection =
		configurations.map { resolve(it) }
			.reduceOrNull { acc, fileCollection -> acc + fileCollection }
			?: project.files()
	
	// no-op
	override fun project(notation: Any): Spec<ResolvedDependency> = Spec<ResolvedDependency> { false }
	override fun dependency(dependencyNotation: Any): Spec<ResolvedDependency> = Spec<ResolvedDependency> { false }
	override fun exclude(spec: Spec<ResolvedDependency>) {}
	override fun include(spec: Spec<ResolvedDependency>) {}
}
