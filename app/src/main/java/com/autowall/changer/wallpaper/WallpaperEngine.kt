package com.autowall.changer.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.autowall.changer.model.AutoWallSettings
import com.autowall.changer.model.FitMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a photo into a wallpaper bitmap (rounded corners + optional tone wash)
 * and hands it to the platform.
 */
object WallpaperEngine {

    data class Screen(val width: Int, val height: Int)

    /** Renders and applies the wallpaper. Safe to call from a background dispatcher. */
    suspend fun apply(context: Context, file: File, settings: AutoWallSettings) =
        withContext(Dispatchers.IO) {
            val screen = screenSize(context)
            val source = decodeScaled(file, screen)
            val radiusPx = settings.cornerRadiusDp * context.resources.displayMetrics.density
            val bitmap = render(source, screen, settings, radiusPx)
            source.recycle()
            try {
                val manager = WallpaperManager.getInstance(context)
                runCatching { manager.suggestDesiredDimensions(screen.width, screen.height) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val flags = if (settings.applyToLockScreen) {
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    } else {
                        WallpaperManager.FLAG_SYSTEM
                    }
                    manager.setBitmap(bitmap, null, true, flags)
                } else {
                    manager.setBitmap(bitmap)
                }
            } catch (security: SecurityException) {
                throw IOException(
                    "Android blocked the wallpaper change: ${security.message}",
                    security,
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

    fun screenSize(context: Context): Screen {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Screen(bounds.width().coerceAtLeast(1), bounds.height().coerceAtLeast(1))
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
            Screen(
                metrics.widthPixels.coerceAtLeast(1),
                metrics.heightPixels.coerceAtLeast(1),
            )
        }
    }

    private fun decodeScaled(file: File, screen: Screen): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("${file.name} is not a readable image")
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= screen.width &&
            bounds.outHeight / (sample * 2) >= screen.height
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("Unable to decode ${file.name}")
    }

    private fun render(
        source: Bitmap,
        screen: Screen,
        settings: AutoWallSettings,
        radiusPx: Float,
    ): Bitmap {
        val width = screen.width
        val height = screen.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        if (settings.fitMode == FitMode.FIT) {
            val blurred = blurredBackdrop(source, width, height)
            val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                shader = BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, backdropPaint)
            blurred.recycle()

            val fitScale = min(width / source.width.toFloat(), height / source.height.toFloat())
            val fitWidth = source.width * fitScale
            val fitHeight = source.height * fitScale
            val fitRect = RectF(
                (width - fitWidth) / 2f,
                (height - fitHeight) / 2f,
                (width + fitWidth) / 2f,
                (height + fitHeight) / 2f,
            )
            val fitPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                    setLocalMatrix(
                        Matrix().apply {
                            setScale(fitScale, fitScale)
                            postTranslate(fitRect.left, fitRect.top)
                        },
                    )
                }
            }
            canvas.drawRoundRect(fitRect, radiusPx, radiusPx, fitPaint)
        } else {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                shader = fillShader(source, width, height)
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)
        }

        val tone = settings.tone
        if (tone.argb != 0L && settings.toneStrength > 0f) {
            val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tone.argb.toInt()
                alpha = (settings.toneStrength.coerceIn(0f, 1f) * 255).toInt()
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, overlay)
        }

        return output
    }

    private fun fillShader(source: Bitmap, width: Int, height: Int): BitmapShader {
        val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
        val dx = (width - source.width * scale) / 2f
        val dy = (height - source.height * scale) / 2f
        return BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(dx, dy)
                },
            )
        }
    }

    /** Cheap blur: render the photo tiny, tint it dark, then scale it back up smoothly. */
    private fun blurredBackdrop(source: Bitmap, width: Int, height: Int): Bitmap {
        val smallWidth = max(12, width / 22)
        val smallHeight = max(12, height / 22)
        val small = Bitmap.createBitmap(smallWidth, smallHeight, Bitmap.Config.ARGB_8888)
        val smallCanvas = Canvas(small)
        smallCanvas.drawRect(
            0f,
            0f,
            smallWidth.toFloat(),
            smallHeight.toFloat(),
            Paint(Paint.FILTER_BITMAP_FLAG).apply { shader = fillShader(source, smallWidth, smallHeight) },
        )
        smallCanvas.drawColor(BACKDROP_SHADE)
        val blurred = Bitmap.createScaledBitmap(small, width, height, true)
        small.recycle()
        return blurred
    }

    private val BACKDROP_SHADE = Color.argb(96, 0, 0, 0)
}
