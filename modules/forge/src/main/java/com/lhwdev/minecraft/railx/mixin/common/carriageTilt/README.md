# How Carriage Tilt Works

I wrote this article so that my brain does not forget about this structure.
As I'm working for lots of things (academic classes, study, lab, service maintenance, other toy projects, ...)
and my brain capacity hit low.

### `DimensionalCarriageEntity.railx$tilt`

Actually, tilt may have be placed in Carriage, i.e. `Carriage.railx$tilt`. But other props like `positionAnchor`
`rotationAnchors` live inside DCE, so `railx$tilt` is also inside DCE.

- `railx$tilt` is `Couple<Double>`, so that it will contain two separate tilt values of leading/trailing point.
  This denotes roll of normal vector, originating from `rotationAnchors`.
  * When carriage is in one dimension, `rotationAnchors` denotes anchor of leading/trailing bogey.
    See `Carriage.updateContraptionAnchors()`.
  * TODO: yaw/pitch should also update due to different tilt of leading/trailing.

- `Carriage.updateContraptionAnchors()` updates every DCE, so that props are applied to
  `TravellingPoint -> positionAnchor, rotationAnchors`.

### `OrientedContraptionEntity.railx$roll` / `getRailx$viewZRot()`

Simple enough, huh?

- `DimensionalCarriageEntity.alignEntity()` applies its roll value to `OrientedContraptionEntity` which is actual
  Minecraft `Entity`. (DCE is not proper 'Entity')

- There are many methods that is related to yaw/pitch/roll.
  `ClientOrientedContraptionEntityMixin.applyRollToLocalTransforms` is what actually affects where contraption is
  rendered.

### TODO: passenger location update

TODO: Currently location of passenger is quite weird. Fix this

### Bogey Rotation

Bogey rotation is handled separately, so I handled this separately via `CarriageBogeyMixin.railx$roll` and
`CarriageContraptionEntityRendererMixin.translateBogey()`.
