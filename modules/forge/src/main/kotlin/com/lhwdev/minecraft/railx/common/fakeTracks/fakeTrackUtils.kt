@file:JvmName("FakeTrackUtils")

package com.lhwdev.minecraft.railx.common.fakeTracks

import com.simibubi.create.content.trains.track.BezierConnection


@Suppress("PropertyName")
internal interface IFakeTrackContainer {
	val `ralix$fakeTrackState`: FakeTrackState
}


val BezierConnection.fakeTrackState: FakeTrackState
	get() = (this as IFakeTrackContainer).`ralix$fakeTrackState`
