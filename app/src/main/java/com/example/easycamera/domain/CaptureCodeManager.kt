package com.example.easycamera.domain

import com.example.easycamera.data.model.CaptureState

object CaptureCodeManager {

    fun formatCode(value: Int): String {
        return if (value < 10) "0${value}" else value.toString()
    }

    fun onFieldCodeChanged(currentState: CaptureState, newFieldCode: Int): CaptureState {
        return currentState.copy(
            fieldCode = newFieldCode.coerceIn(1, 99),
            sampleCode = 1,
            currentAngleIndex = 0,
            capturedAngles = emptyList(),
            isGroupComplete = false
        )
    }

    fun onSampleCodeChanged(currentState: CaptureState, newSampleCode: Int): CaptureState {
        return currentState.copy(
            sampleCode = newSampleCode.coerceIn(1, 99),
            currentAngleIndex = 0,
            capturedAngles = emptyList(),
            isGroupComplete = false
        )
    }

    fun onAngleIndexChanged(currentState: CaptureState, newIndex: Int): CaptureState {
        return currentState.copy(
            currentAngleIndex = newIndex.coerceIn(0, 3),
            isGroupComplete = false
        )
    }

    fun findNextUncapturedAngleIndex(
        angleSequence: List<String>,
        capturedAngles: List<String>
    ): Int {
        for (i in angleSequence.indices) {
            if (angleSequence[i] !in capturedAngles) {
                return i
            }
        }
        return 0
    }

    /**
     * Returns null if the current angle is already captured (no state change).
     * Returns a new [CaptureState] if capture was successful.
     */
    fun simulateCapture(currentState: CaptureState, angleSequence: List<String>): CaptureState? {
        val currentAngle = angleSequence.getOrElse(currentState.currentAngleIndex) { return currentState }

        if (currentAngle in currentState.capturedAngles) {
            return null
        }

        val newCapturedAngles = currentState.capturedAngles + currentAngle
        val allComplete = angleSequence.all { it in newCapturedAngles }

        return if (allComplete) {
            currentState.copy(
                capturedAngles = newCapturedAngles,
                isGroupComplete = true
            )
        } else {
            val nextIndex = findNextUncapturedAngleIndex(angleSequence, newCapturedAngles)
            currentState.copy(
                capturedAngles = newCapturedAngles,
                currentAngleIndex = nextIndex,
                isGroupComplete = false
            )
        }
    }

    fun confirmGroup(currentState: CaptureState): CaptureState {
        return currentState.copy(
            sampleCode = currentState.sampleCode + 1,
            currentAngleIndex = 0,
            capturedAngles = emptyList(),
            isGroupComplete = false
        )
    }

    fun retakeGroup(currentState: CaptureState): CaptureState {
        return currentState.copy(
            currentAngleIndex = 0,
            capturedAngles = emptyList(),
            isGroupComplete = false
        )
    }
}