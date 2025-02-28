package com.adsperclick.media.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import com.adsperclick.media.R
import com.adsperclick.media.utils.Validate.toInitials
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.TypedValue
import android.webkit.MimeTypeMap
import com.adsperclick.media.data.dataModels.GroupChatListingData
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.absoluteValue

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

    fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "Syncing... (check n/w connection)"

        val date = timestamp.toDate() // Convert Firestore Timestamp to Java Date
        val sdf = SimpleDateFormat("dd MMM, yyyy  hh:mm a", Locale.getDefault()) // Format
        return sdf.format(date)
    }

    fun formatNotificationTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "N/A"

        val date = timestamp.toDate()
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        calendar.time = date

        return when {
            // If the notification was sent today
            isSameDay(calendar, today) -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)  // "3:30 PM"
            }
            // If it was sent yesterday
            isYesterday(calendar, today) -> {
                "Yesterday, " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
            }
            // If it was sent within the last 7 days
            isWithinLastWeek(calendar, today) -> {
                SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault()).format(date)  // "Monday, 3:30 PM"
            }
            // If it's older than a week
            else -> {
                SimpleDateFormat("dd MMM, yyyy  hh:mm a", Locale.getDefault()).format(date)  // "15 Jan, 2025 3:30 PM"
            }
        }
    }

    // Helper functions
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        cal2.add(Calendar.DAY_OF_YEAR, -1)  // Move to yesterday
        return isSameDay(cal1, cal2)
    }

    fun isWithinLastWeek(cal1: Calendar, cal2: Calendar): Boolean {
        cal2.add(Calendar.DAY_OF_YEAR, 7)  // Move back to original
        return cal1.after(cal2.apply { add(Calendar.DAY_OF_YEAR, -7) })  // Compare within last 7 days
    }


    // Convert Long (milliseconds) to Firestore Timestamp
    fun longToTimestamp(timeInMillis: Long): Timestamp {
        return Timestamp(Date(timeInMillis))
    }

    // Convert Firestore Timestamp to Long (milliseconds)
    fun timestampToLong(timestamp: Timestamp?): Long {
        return timestamp?.toDate()?.time ?: 0L
    }

    val senderColors = listOf(
        Color.parseColor("#137333"), // Dark Green
        Color.parseColor("#174EA6"), // Dark Blue
        Color.parseColor("#B06000"), // Dark Orange
        Color.parseColor("#5E35B1"), // Dark Purple
        Color.parseColor("#C5221F")  // Dark Red
    )

    fun getSenderColor(userId: String?): Int {
        val index = userId.hashCode().absoluteValue % senderColors.size
        return senderColors[index]
    }

    fun formatMessageTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Example: 01:45 PM
        return formatter.format(date)
    }

    private const val MAX_IMAGE_SIZE = 200 * 1024
    const val CURRENT_DATE_FORMAT: String = Constants.yyyyMMdd_HHmmss
    const val NA = "NA"

    fun compressFile(context: Context, file: File): File {
        if (file.length() > MAX_IMAGE_SIZE) {
            val newFile = createImageFile(context)
            var streamLength = MAX_IMAGE_SIZE
            var compressQuality = 105
            val bmpStream = ByteArrayOutputStream()
            try {
                while (streamLength >= MAX_IMAGE_SIZE && compressQuality > 5) {
                    bmpStream.use {
                        it.flush()
                        it.reset()
                    }
                    compressQuality -= 5
                    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options())?.let {
                        it.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
                        val bmpPicByteArray = bmpStream.toByteArray()
                        streamLength = bmpPicByteArray.size
                    }
                }
                FileOutputStream(newFile).use {
                    it.write(bmpStream.toByteArray())
                }
                return newFile
            } catch (e: Exception) {

                return file
            }
        }
        return file
    }

    fun createImageFile(context: Context): File {
        val timeStamp: String =
            SimpleDateFormat(CURRENT_DATE_FORMAT, Locale.getDefault()).format(
                Date()
            )
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPG_${timeStamp}_",
            Constants.JPG,
            storageDir
        )
    }


// Inside the UtilityFunctions object, add these methods:

    /**
     * Checks if the file is a video based on its extension
     */
    fun isVideoFile(file: File): Boolean {
        val extension = file.extension.lowercase(Locale.ROOT)
        return extension in listOf("mp4", "avi", "mov", "mkv", "3gp", "webm")
    }

    /**
     * Gets the MIME type of a file
     */
    fun getMimeType(file: File): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    /**
     * Creates a video file
     */
    fun createVideoFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat(CURRENT_DATE_FORMAT, Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile(
            "VID_${timeStamp}_",
            ".mp4",
            storageDir
        )
    }

    private fun createTempFile(context: Context, name: String?): File {
        val storageDir =
            context.externalCacheDir ?: context.getExternalFilesDir(null) ?: context.cacheDir

        val fileName: String = if (name != null && name.isNotEmpty()) {
            "${Date().time}-$name".replace(Constants.BACK_SLASH, Constants.DASH)
        } else {
            "${UUID.randomUUID()}"
        }

        // Using Java's File.createTempFile instead of Kotlin's deprecated function
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // For Android O and above, use the nio Files API
            val prefix = "temp_${Date().time}_"
            val suffix = if (name != null) "_$name" else null
            val path = Files.createTempFile(Paths.get(storageDir.path), prefix, suffix)
            path.toFile()
        } else {
            // For older Android versions, use Java's File.createTempFile with appropriate permissions
            val tempFile = java.io.File.createTempFile("temp_${Date().time}_",
                if (name != null) "_$name" else null,
                storageDir)

            // Set permissions to be more restrictive (readable/writable only by the app)
            tempFile.setReadable(true, true)  // Readable only by owner
            tempFile.setWritable(true, true)  // Writable only by owner
            tempFile
        }
    }

    /**
     * Gets a thumbnail from a video file
     */
    fun getVideoThumbnail(context: Context, videoFile: File): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(videoFile, android.util.Size(320, 240), null)
            } else {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoFile.path)
                val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                frame
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Updated saveFileFromUri method to handle both images and videos
     */
    fun saveFileFromUri(context: Context, uri: Uri): File? {
        try {
            val initialStream = context.contentResolver.openInputStream(uri) ?: return null
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val mimeType = context.contentResolver.getType(uri)

            // Create appropriate file based on type
            val targetFile = if (mimeType?.startsWith("video/") == true) {
                createVideoFile(context)
            } else if (mimeType?.startsWith("image/") == true) {
                createImageFile(context)
            } else {
                // For PDFs and other files
                createTempFile(context, docFile?.name)
            }

            // Copy the file data
            val buffer = ByteArray(initialStream.available())
            initialStream.read(buffer)
            val outStream = FileOutputStream(targetFile)
            outStream.write(buffer)
            outStream.close()
            initialStream.close()

            // Compress if it's an image (don't compress videos)
            return if (mimeType?.startsWith("image/") == true) {
                compressFile(context, targetFile)
            } else {
                targetFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Add a method to load video thumbnails with Glide
     */
    fun setVideoThumbnailOnImageView(context: Context, videoFile: File, imageView: ImageView) {
        val thumbnail = getVideoThumbnail(context, videoFile)
        if (thumbnail != null) {
            Glide.with(context)
                .load(thumbnail)
                .error(R.drawable.baseline_person_24)
                .placeholder(R.drawable.baseline_person_24)
                .fitCenter()
                .into(imageView)
        } else {
            // If thumbnail extraction fails, load a video placeholder
            imageView.setImageResource(R.drawable.baseline_person_24)  // Consider using a video-specific placeholder
        }
    }

    fun loadImageWithGlide(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        placeholderId: Int = R.drawable.baseline_person_24,
        errorId: Int = R.drawable.baseline_person_24
    ) {
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(placeholderId)
            return
        }

        Glide.with(context)
            .load(imageUrl)
            .placeholder(placeholderId)
            .error(errorId)
            .centerCrop()
            .into(imageView)
    }

    fun setInitialsDrawable(imageView: ImageView, name:String?) {
        if (name.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.baseline_person_24)
            return
        }
        val drawable = generateInitialsDrawable(
            imageView.context, name ?: "NA")
        imageView.setImageDrawable(drawable)
    }

    fun dp2px(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }


    fun gcListDateFormat(timestamp: Long): String {
        val date = Date(timestamp)
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        // Check if the date is today
        if (calendar.apply { time = date }.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        ) {
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date) // Example: 9:26 AM
        }

        // Check if the date is yesterday
        today.add(Calendar.DAY_OF_YEAR, -1)
        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Yesterday"
        }

        // Otherwise, return date in dd/MM/yy format
        return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date) // Example: 23/02/25
    }
}