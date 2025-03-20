package com.adsperclick.media.applicationCommonView.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.min

class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#2196F3") // Blue color matching the border
        strokeWidth = 4f
    }

    private val random = Random()
    private val barCount = 5
    private val barHeights = FloatArray(barCount)
    private var animator: Runnable? = null

    init {
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = Runnable {
            // Randomize bar heights
            for (i in 0 until barCount) {
                barHeights[i] = random.nextFloat() * 0.8f + 0.2f // Between 0.2 and 1.0
            }

            invalidate()
            postDelayed(animator, 150) // Update every 150ms for a natural audio wave effect
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animator)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(animator)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val barWidth = 8f
        val gap = (width - (barCount * barWidth)) / (barCount + 1)

        var startX = gap

        for (i in 0 until barCount) {
            val barHeight = height * barHeights[i]
            val top = (height - barHeight) / 2

            canvas.drawLine(
                startX + barWidth / 2,
                top,
                startX + barWidth / 2,
                top + barHeight,
                paint
            )

            startX += barWidth + gap
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility == VISIBLE) {
            post(animator)
        } else {
            removeCallbacks(animator)
        }
    }
}