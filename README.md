<<<<<<< HEAD
# RailX

🚧 [WIP] Minecraft Mod for Create Forge: train track which can be placed at any angle.

Some code are based on [Create Computing](https://github.com/Sascha-T/create-computing).

## Features (not complete)

### FlexiTrack

- Can be placed at any angle.
- Can be used to implement realistic railroad.

### Remote Train Observer

- Can be observed by any other Track Network Observer in same track graph.
- Has unique id inside track graph.
- To mitigate chunk unloading problem in previous train observer.


### Track Network Observer

- Can be used by CC
  * Read graph
- Can be used by itself
  * Remote observer reading mode: outputs redstone signal
  * Train tracking mode: can read occupied blocks, speed, metadata.
  * Change on-graph node info; such as TVM transmitter info
  * Block mode: can detect if a signal block is occupied
  * Transmission mode: transmit a data on-air into train

### Train Observer (addon)

- Also detects train speed

### TVM Transmitter

- Upgrade version of Train Observer
- Receives/Transmits data from/to train: static data or lua evaluated data (works even if chunk is unloaded)
  * balise -> train: speed limit, pantograph asc/desc, branch info, gradient etc.
  * train -> balise: current speed, train identity, etc
- Also works as CC peripheral
- Also detects train speed
- Use comparator to detect existing train

### Train Signal (addon)

- Use name tag to name block node
