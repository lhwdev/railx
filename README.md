# RailX

Note that this mod is just a toy project. Nothing is expected to work well so be sure to backup your world before using
this mod.

## Features

### `flexiTrack`

Stage: **Alpha**; most things are implemented, but not tested thoroughly. Some known bugs exist.

Allows train tracks to be placed at any angle.

### `realisticSpeed`

Stage: **Alpha**; most things are implemented, but not tested thoroughly.

Implement real-life like train movement. Includes dynamic calculation of gravitational acceleration, curvature
resistance, air resistance, wheel slip. However implementation of air resistance is very crude; will at least need some
kind of simplest CFD.

Most aspect of this properties is configurable. Note that `realisticSpeed` considers one block length as one meter, and
1 second (=20 game tick) as one second. So something will seem to work weird; if you build curve as you would normally
build, train won't be able to speed up as its curvature is too steep. In real life, minimum curve radius of railway is
much larger than expected; minimum radii of some high speed train railway is 8000 m. Of course, I'm planning to apply
different curvature resistance according to gauge; monorail track of Steam 'n' Rails should have less resistance than
standard gauge.

### `splitGraph`

Allows 'splitting' track graph into two or more. Uses specialized `Split Track Block` to separate graph from one end to
another. This will help improving performance of modifying railway on huge graph.

### 🚧 `carriageMetadata`

Stage: **Nothing Done**

### 🚧 `cab`

Stage: **Nothing Done**

Train control GUIs for more realistic train control.
Can be integrated with ComputerCraft to provide custom GUI.

### 🚧 `signal`

Stage: **Nothing Done**

Implementing ATS/ATP,ETCS/ATC,TVM etc. Or CTCS controlled by ComputerCraft? (this is scope of ccIntegration)

### 🚧 `ccIntegration`

Stage: **Nothing Done**

### 🚧 `realisticSound`

Stage: **Nothing Done**; Note that this is just my imagination. I cannot write these thing (maybe). There are no
suitable
VVVF implementation in Github as far as I know.

Train sound simulation via realistic physical simulation. Pseudo-aerodynamic simulation for steam and diesel, VVVF
simulation for electric train.

### 🚧 `realisticShape`

Stage: **Nothing Done**; seems much harder than I expected. This is also my dream-car.

- Implement real-world like railroad switch
  * integrate with Steam 'n Rails, use railroad switch block to determine path, which will be shown by track shape.
    Switching takes some time, and animation will reflect this.
  * Implements switch rod, wing rail, guard rail, etc.
  * Requires procedural track curve rendering.

Note that track model in Create is much more complicated than I expected; it isn't some combination of rectangles.
They consists of quite- complicated .obj files.

### 🚧 ~~`physicalMovement`~~

Will be implemented if some method of allowing kinetics inside train carriage exists.
