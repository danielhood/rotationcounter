package com.danielhood.rotationcounter

class FpsStats {
    private var currentFps: Float = 0f
    private var frameCounter = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    companion object {
        private const val TAG = "FpsStats"
        private const val frameCount: Int = 10
    }
    fun frameTick(rotationCountStats: RotationCountStats) {
        if (++frameCounter % frameCount == 0) {
            frameCounter = 0
            val now = System.currentTimeMillis()
            val delta = now - lastFpsTimestamp
            currentFps = 1000 * frameCount.toFloat() / delta

            //Log.d(
            //    TAG,
            //    "FPS: ${"%.02f".format(currentFps)}"
            //)

            lastFpsTimestamp = now

            rotationCountStats.lastComputedFps = currentFps
        }
    }
}