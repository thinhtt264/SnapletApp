package com.thinh.snaplet.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    private const val JPEG_QUALITY = 90
    /**
     * Max dimension (long edge) before downscaling. Chosen to match feed display:
     * MediaPage shows post image in 400.dp height × full width (~1000–1200px × 1080–1440px).
     * 1920px is enough for sharp 1:1 on typical phones and keeps back-camera processing fast.
     */
    private const val MAX_DIMENSION = 1920

    /**
     * Applies orientation (EXIF + optional horizontal flip), downscales if over [MAX_DIMENSION], keeps JPEG.
     * Decodes at reduced size via [inSampleSize] first to save memory and time (no full-size decode).
     *
     * @param file Source image (e.g. JPEG from camera)
     * @param flipHorizontal true = mirror (front camera), false = EXIF normalization only
     * @return Path to the output .jpg file, or null on failure (original file unchanged)
     */
    fun flipAndCompressImage(file: File, flipHorizontal: Boolean = false): String? {
        if (!file.exists() || !file.canRead()) return null
        val outputFile = File(file.parent, "${file.nameWithoutExtension}.jpg")
        val path = file.absolutePath
        return try {
            val (boundsW, boundsH) = decodeBounds(path) ?: return null
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = matrixForExifOrientation(orientation)
            if (flipHorizontal) matrix.postScale(-1f, 1f)
            val rect = RectF(0f, 0f, boundsW.toFloat(), boundsH.toFloat())
            matrix.mapRect(rect)
            matrix.postTranslate(-rect.left, -rect.top)
            val outWidth = rect.width().toInt().coerceAtLeast(1)
            val outHeight = rect.height().toInt().coerceAtLeast(1)
            val inSampleSize = computeInSampleSize(maxOf(outWidth, outHeight), MAX_DIMENSION)

            val bitmap = decodeWithSampleSize(path, inSampleSize) ?: return null
            val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()

            FileOutputStream(outputFile).use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            result.recycle()

            ExifInterface(outputFile.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            Logger.e(e, "flipAndCompressImage failed: $path")
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }

    /** Decode only bounds; returns (width, height) or null. */
    private fun decodeBounds(path: String): Pair<Int, Int>? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            if (outWidth <= 0 || outHeight <= 0) null else Pair(outWidth, outHeight)
        }
    }

    /** Decode with inSampleSize; RGB_565 since we encode to JPEG (no alpha). */
    private fun decodeWithSampleSize(path: String, inSampleSize: Int): Bitmap? {
        return BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }.let { BitmapFactory.decodeFile(path, it) }
    }

    /** Smallest power-of-2 inSampleSize so that maxDimension / inSampleSize <= maxTarget. */
    private fun computeInSampleSize(maxDimension: Int, maxTarget: Int): Int {
        if (maxDimension <= maxTarget) return 1
        var sampleSize = 1
        while ((maxDimension / sampleSize) > maxTarget) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun matrixForExifOrientation(orientation: Int): Matrix {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            else -> { /* ORIENTATION_NORMAL or unknown: identity */
            }
        }
        return matrix
    }

    /** Deletes the file at [filePath]. Returns true if deleted. */
    fun deleteFileFromPath(filePath: String?): Boolean {
        if (filePath == null) {
            return false
        }

        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Logger.w("⚠️ Failed to delete file (file.delete() returned false): $filePath")
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e(e, "❌ Failed to delete file: $filePath")
            false
        }
    }

    /** Deletes each file at [filePaths]. Returns count of deleted files. */
    fun deleteFilesFromPaths(filePaths: List<String?>): Int {
        return filePaths.count { deleteFileFromPath(it) }
    }
}
