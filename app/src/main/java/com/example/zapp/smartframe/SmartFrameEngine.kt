package com.example.zapp.smartframe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

enum class DisplayMode(val label: String, val shortDesc: String) {
    SMART_FRAME("Smart Frame", "AI Focus"),
    FIT("Scale: Fit", "Full Photo"),
    FILL("Scale: Fill", "Center Crop")
}

data class SubjectBox(
    val label: String,
    val score: Float,
    val box: RectF // normalized [0..1]
)

data class SmartFrameData(
    val hasSubject: Boolean,
    val subjectLabel: String,
    val badgeText: String,
    val scale: Float,
    val offset: Offset,
    val rawBoxes: List<SubjectBox> = emptyList()
)

object SmartFrameEngine {
    private var detector: ObjectDetector? = null
    private var isInitialized = false

    @Synchronized
    private fun getDetector(context: Context): ObjectDetector? {
        if (!isInitialized) {
            try {
                val baseOptions = BaseOptions.builder()
                    .useNnapi()
                    .build()

                val options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMaxResults(8)
                    .setScoreThreshold(0.20f)
                    .build()

                detector = ObjectDetector.createFromFileAndOptions(
                    context.applicationContext,
                    "efficientdet_lite0.tflite",
                    options
                )
            } catch (e: Exception) {
                // Fallback to CPU if NNAPI is unsupported
                try {
                    val options = ObjectDetector.ObjectDetectorOptions.builder()
                        .setMaxResults(8)
                        .setScoreThreshold(0.20f)
                        .build()
                    detector = ObjectDetector.createFromFileAndOptions(
                        context.applicationContext,
                        "efficientdet_lite0.tflite",
                        options
                    )
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            isInitialized = true
        }
        return detector
    }

    suspend fun analyze(
        context: Context,
        source: Any, // File or Uri
        containerSize: IntSize
    ): SmartFrameData = withContext(Dispatchers.Default) {
        if (containerSize.width <= 0 || containerSize.height <= 0) {
            return@withContext SmartFrameData(
                hasSubject = false,
                subjectLabel = "none",
                badgeText = "Standard Fit",
                scale = 1f,
                offset = Offset.Zero
            )
        }

        // 1. Decode image bounds first
        val (origW, origH) = decodeImageBounds(context, source)
        if (origW <= 0 || origH <= 0) {
            return@withContext defaultFit(containerSize, 800, 480)
        }

        // 2. Load downscaled bitmap for fast ML inference (<30ms)
        val sampleSize = calculateInSampleSize(origW, origH, 512, 512)
        val bitmap = decodeSampledBitmap(context, source, sampleSize)

        if (bitmap == null) {
            return@withContext defaultFit(containerSize, origW, origH)
        }

        // 3. Run on-device ObjectDetector
        val det = getDetector(context)
        val detections = mutableListOf<SubjectBox>()

        if (det != null) {
            try {
                val tensorImage = TensorImage.fromBitmap(bitmap)
                val results: List<Detection> = det.detect(tensorImage)

                for (d in results) {
                    val cat = d.categories.firstOrNull() ?: continue
                    val label = cat.label.lowercase()
                    val score = cat.score

                    // Convert pixel boundingBox in sample bitmap to normalized [0..1]
                    val b = d.boundingBox
                    val normBox = RectF(
                        (b.left / bitmap.width).coerceIn(0f, 1f),
                        (b.top / bitmap.height).coerceIn(0f, 1f),
                        (b.right / bitmap.width).coerceIn(0f, 1f),
                        (b.bottom / bitmap.height).coerceIn(0f, 1f)
                    )
                    detections.add(SubjectBox(label, score, normBox))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Filter for relevant subjects (prioritize cats, then pets, then people/vehicles)
        val cats = detections.filter { it.label == "cat" }
        val dogs = detections.filter { it.label == "dog" }
        val people = detections.filter { it.label == "person" }
        val others = detections.filter { it.label !in listOf("cat", "dog", "person") }

        val (targetBoxes, badgeText, primaryLabel) = when {
            cats.size >= 2 -> Triple(cats, "👥 ${cats.size} Cats Focused", "cat")
            cats.size == 1 -> Triple(cats, "🐾 Cat Focused", "cat")
            dogs.size >= 2 -> Triple(dogs, "🐶 ${dogs.size} Dogs Focused", "dog")
            dogs.size == 1 -> Triple(dogs, "🐶 Dog Focused", "dog")
            people.isNotEmpty() -> Triple(people, "👤 Subject Focused", "person")
            others.isNotEmpty() -> {
                val best = others.maxByOrNull { it.score }
                Triple(listOfNotNull(best), "🎯 ${best?.label?.replaceFirstChar { it.uppercase() }} Focused", best?.label ?: "subject")
            }
            else -> Triple(emptyList(), "Auto Balanced", "none")
        }

        if (targetBoxes.isEmpty()) {
            // Fallback: If portrait image on landscape screen, center on upper half (portrait rule of thirds)
            return@withContext calculateFallbackFraming(containerSize, origW, origH)
        }

        // 5. Combine bounding boxes (Union Box)
        val unionLeft = targetBoxes.minOf { it.box.left }
        val unionTop = targetBoxes.minOf { it.box.top }
        val unionRight = targetBoxes.maxOf { it.box.right }
        val unionBottom = targetBoxes.maxOf { it.box.bottom }

        val isCat = (primaryLabel == "cat")
        val (calcScale, calcOffset) = solveSmartFraming(
            containerSize = containerSize,
            origW = origW,
            origH = origH,
            unionBox = RectF(unionLeft, unionTop, unionRight, unionBottom),
            isCat = isCat
        )

        SmartFrameData(
            hasSubject = true,
            subjectLabel = primaryLabel,
            badgeText = badgeText,
            scale = calcScale,
            offset = calcOffset,
            rawBoxes = targetBoxes
        )
    }

    /**
     * Solves optimal scale and offset to frame the subject box into containerSize
     */
    private fun solveSmartFraming(
        containerSize: IntSize,
        origW: Int,
        origH: Int,
        unionBox: RectF,
        isCat: Boolean
    ): Pair<Float, Offset> {
        val Wc = containerSize.width.toFloat()
        val Hc = containerSize.height.toFloat()

        // Size and position in ContentScale.Fit
        val fitScale = min(Wc / origW, Hc / origH)
        val drawnW = origW * fitScale
        val drawnH = origH * fitScale
        val drawnLeft = (Wc - drawnW) / 2f
        val drawnTop = (Hc - drawnH) / 2f

        // Subject coordinates in container space at scale=1.0
        val sLeft = drawnLeft + unionBox.left * drawnW
        val sTop = drawnTop + unionBox.top * drawnH
        val sRight = drawnLeft + unionBox.right * drawnW
        val sBottom = drawnTop + unionBox.bottom * drawnH

        val sW = max(1f, sRight - sLeft)
        val sH = max(1f, sBottom - sTop)

        // Add safe headroom (15% for cat ears) and breathing room
        val padding = if (isCat) 1.25f else 1.20f
        val targetBoxW = sW * padding
        val targetBoxH = sH * padding

        // Scale needed so that the framed box comfortably fills ~75% of viewport
        val scaleH = (Wc * 0.75f) / targetBoxW
        val scaleV = (Hc * 0.85f) / targetBoxH
        val targetScale = max(1.15f, min(scaleH, scaleV)).coerceIn(1.0f, 3.2f)

        // Eye line / focal point
        // For cats, eyes are at ~35% of head/body height from the top
        val focalX = (sLeft + sRight) / 2f
        val focalY = if (isCat) sTop + sH * 0.35f else (sTop + sBottom) / 2f

        // Desired placement: Rule of Thirds
        // Place focal point at 38% height of the Cadillac CTS display
        val desiredTargetX = Wc / 2f
        val desiredTargetY = Hc * 0.40f

        // Calculate offset so focal point lands at desiredTarget
        val centerX = Wc / 2f
        val centerY = Hc / 2f

        val transX = desiredTargetX - (centerX + (focalX - centerX) * targetScale)
        val transY = desiredTargetY - (centerY + (focalY - centerY) * targetScale)

        // Bounded within container edges
        val maxOffsetX = (Wc * (targetScale - 1f)) / 2f
        val maxOffsetY = (Hc * (targetScale - 1f)) / 2f

        val clampedOffsetX = transX.coerceIn(-maxOffsetX, maxOffsetX)
        val clampedOffsetY = transY.coerceIn(-maxOffsetY, maxOffsetY)

        return Pair(targetScale, Offset(clampedOffsetX, clampedOffsetY))
    }

    private fun calculateFallbackFraming(
        containerSize: IntSize,
        origW: Int,
        origH: Int
    ): SmartFrameData {
        val Wc = containerSize.width.toFloat()
        val Hc = containerSize.height.toFloat()
        val isPortrait = origH > origW

        if (isPortrait && Wc > Hc) {
            // Portrait photo on landscape screen: gentle 1.4x zoom biased toward top half
            val targetScale = 1.35f
            val maxOffsetY = (Hc * (targetScale - 1f)) / 2f
            val offsetY = maxOffsetY * 0.5f // shift up to show face / upper subject

            return SmartFrameData(
                hasSubject = false,
                subjectLabel = "balanced",
                badgeText = "📐 Smart Framing",
                scale = targetScale,
                offset = Offset(0f, offsetY)
            )
        } else {
            return defaultFit(containerSize, origW, origH)
        }
    }

    private fun defaultFit(containerSize: IntSize, origW: Int, origH: Int): SmartFrameData {
        return SmartFrameData(
            hasSubject = false,
            subjectLabel = "none",
            badgeText = "Scale: Fit",
            scale = 1f,
            offset = Offset.Zero
        )
    }

    private fun decodeImageBounds(context: Context, source: Any): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            when (source) {
                is File -> BitmapFactory.decodeFile(source.absolutePath, options)
                is Uri -> {
                    context.contentResolver.openInputStream(source)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                }
            }
            return Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            return Pair(0, 0)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmap(context: Context, source: Any, inSampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return try {
            when (source) {
                is File -> BitmapFactory.decodeFile(source.absolutePath, options)
                is Uri -> {
                    context.contentResolver.openInputStream(source)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
