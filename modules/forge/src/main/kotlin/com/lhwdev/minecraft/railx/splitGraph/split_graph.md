# About `splitGraph`

## Limitation

- Cannot put Split Train Track Block in a row; usually you don't have to.
- Cannot relocate train over splitting point.
- Still has some minor bugs I could not find

## What's going under the hood

There is special type of `TrackNode` called [SplittingTrackNode](SplittingTrackNode.kt). This node works like below:

- There is two graph, `A` and `B`. We want splitting point to be at location `loc`.
- `A.locateNode(loc) == SplittingTrackNode(otherGraph = B.id, ...)`, called `nodeA`.
- `B.locateNode(loc) == SplittingTrackNode(otherGraph = A.id, ...)`, called `nodeB`.
- When having `nodeA`, can find corresponding split node by
  `Create.RAILWAYS.trackNetworks.get(nodeA.otherGraph).locateNode(nodeA.location)`.

---

**TravellingPoint** handles splitting node by following inside `travel()`:

1. When encountering end of track, test if end node is `SplittingTrackNode`.
2. If so, teleport point into other graph, setting `destinationGraph` with other graph.
3. Travel remaining distance in other graph.

---

**Train** handles splitting node by following:

- After all `TravellingPoint`s are updated, check any point was teleported, by checking their `destinationGraph`.

- Update `Train.graph` corresponding to following:
  - Let `found` be set containing all `destinationGraph`s.
  - If `found.size == 1`, set `Train.graph` with that.
  - If `found.contains(null)`, replace all `destinationGraph == null` to `train.graph`.
  - Set `train.graph` to `MergedTrackGraph(graphs = found)`.

- Some behaviors to be careful about:
  - If `train.graph` is set by other, all `TravellingPoint.destinationGraph` is reset. To preserve points, use
    `replaceGraphPreserving(from, to)` from `:api` or mod jar.
  - While train is in split state (spans multiple graphs), `train.graph` becomes custom graph implementation,
    `MergedTrackGraph`.
  - When train moves from `graph A` to `graph B` to enter split state, `graph A` takes priority. That means, some
    mutating methods like `train.graph.addNode` will happen on `graph A`. While train is in this split state, this order
    won't change, even when moving opposite to direction train entered split state.

---

All implementation of **SplitGraphTrack** handles by following:

- Implement `getConnected()` by:
  - Does not add any points returned by `linear=true`, each end of axis.
  - Add all from `connections`. (of course, if `linear=false`)
