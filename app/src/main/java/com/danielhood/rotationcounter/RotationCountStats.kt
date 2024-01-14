package com.danielhood.rotationcounter

class RotationCountStats {
    var rotationCount: Int = 0
    var lastComputedFps: Float = 0f

    private var detected = false

    fun update(colorMatched: Boolean) {
        // Check if still no match
        if (!detected && !colorMatched) return

        // Check if still matched
        if (detected && colorMatched) return

        // Check if to change to not detected
        if (detected && !colorMatched) {
            detected = false
            return
        }

        // Current state is !detected && colorMatched, change to detected and increment count
        detected = true
        rotationCount++
    }
}