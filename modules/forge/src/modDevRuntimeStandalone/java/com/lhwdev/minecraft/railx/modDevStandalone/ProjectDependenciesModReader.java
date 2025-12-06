package com.lhwdev.minecraft.railx.modDevStandalone;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;


// see NestedLibraryModReader
public class ProjectDependenciesModReader implements IModFileReader {
	private final Set<String> projectDependencies;
	
	public ProjectDependenciesModReader() {
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
	public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
		if(!projectDependencies.contains(jar.getPrimaryPath().toAbsolutePath().toString())) return null;
		return IModFile.create(
			SecureJar.from(jar),
			JarModsDotTomlModFileReader::manifestParser,
			IModFile.Type.GAMELIBRARY,
			attributes
		);
	}
	
	@Override
	public String toString() {
		return "project dependencies mod provider";
	}
	
	@Override
	public int getPriority() {
		return -100;
	}
}
