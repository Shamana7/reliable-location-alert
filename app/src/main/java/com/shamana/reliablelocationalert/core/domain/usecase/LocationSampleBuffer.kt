package com.shamana.reliablelocationalert.core.domain.usecase

import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import java.util.ArrayDeque

class LocationSampleBuffer(
    private val maxSize: Int = 5
) {

    private val buffer = ArrayDeque<LocationSample>()

    fun add(sample: LocationSample) {
        if(isFull()){
            buffer.removeFirst()
        }
        buffer.addLast(sample)
    }

    fun samples(): List<LocationSample> = buffer.toList()

    fun isFull(): Boolean = buffer.size == maxSize
}
