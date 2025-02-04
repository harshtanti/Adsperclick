package com.adsperclick.media.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.adsperclick.media.utils.Validate.toInitials
import kotlin.math.abs

object UtilityFunctions {

    // Generate drawable with initials (For people/group-chats
    // which don't have profile-pictures, there name initials
    // will be displayed as their DP, Just like it happens in mobile phone's Phone app
    fun generateInitialsDrawable(
        context: Context,
        name: String,
        sizeDp: Int = 45,
        textColor: Int = Color.WHITE
    ): Drawable {
        // Extract initials (first letter of each word)
        val initials = name.toInitials()
        val backgroundColor = getColorForName(name)

        // Create a TextDrawable
        val paint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textDensityFactor = if(initials.length == 1) 2f else 2.2f     // Because if profile-pic has
        // only one char, then it can be bigger, but if it has 2 chars, then it should be slightly smaller

        val textPaint = Paint().apply {
            color = textColor
            textSize = sizeDp * context.resources.displayMetrics.density / textDensityFactor
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                val width = bounds.width()
                val height = bounds.height()

                // Draw circular background
                canvas.drawCircle(width/2f, height/2f, width/2f, paint)

                // Draw text
                val xPos = width / 2f
                val yPos = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(initials, xPos, yPos, textPaint)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }

    // To pick a random decent color for Profile Pic
    private fun getColorForName(name: String): Int {
        // Predefined color palette with distinct, pleasant colors
        val colors = listOf(
            Color.parseColor("#FF5733"),  // Vibrant Orange-Red
            Color.parseColor("#3498DB"),  // Soft Blue
            Color.parseColor("#2ECC71"),  // Emerald Green
            Color.parseColor("#9B59B6"),  // Purple
            Color.parseColor("#F39C12"),  // Sunflower Yellow
            Color.parseColor("#E74C3C"),  // Brick Red
            Color.parseColor("#1ABC9C"),  // Turquoise
            Color.parseColor("#34495E"),  // Dark Blue-Gray
            Color.parseColor("#D35400"),  // Pumpkin Orange
            Color.parseColor("#2980B9")   // Deep Blue
        )

        // Generate a consistent color based on the name's hash
        return colors[abs(name.hashCode()) % colors.size]
    }
}