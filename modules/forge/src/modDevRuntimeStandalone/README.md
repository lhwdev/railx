All these dummy things are for adding project dependency jars into runtime classpath. If project jars are added
normally, they are not picked up by `IModFileReader` (especially `NestedLibraryModReader`).

`modDevRuntimeStandalone` source set is compiled and packaged into separate jar, which is included into
`legacyClasspath`. `FMLModLoader` will pick up this
[ProjectDependenciesModReader](./java/com/lhwdev/minecraft/railx/modDevStandalone/ProjectDependenciesModReader.java).
