package com.lhwdev.minecraft.railx.modDevStandalone;

import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import net.minecraftforge.forgespi.locating.IModFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


// see NestedLibraryModReader
public class ProjectDependenciesLocator extends AbstractJarFileModLocator {
	private final Set<String> projectDependencies;
	
	public ProjectDependenciesLocator() {
		super();
		
		var dependenciesFile = System.getProperty("railx.project_dependencies_file");
		if(dependenciesFile == null)
			throw new IllegalStateException("property railx.project_dependencies_file required");
		
		try {
			projectDependencies = new HashSet<>(Files.readAllLines(Path.of(dependenciesFile)));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Stream<Path> scanCandidates() {
		return projectDependencies.stream().map(Path::of);
	}
	
	@Override
	protected String getDefaultJarModType() {
		return IModFile.Type.GAMELIBRARY.name();
	}
	
	@Override
	public String name() {
		return "project dependencies";
	}
	
	@Override
	public void initArguments(Map<String, ?> arguments) {}
}
